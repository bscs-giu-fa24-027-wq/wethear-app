import React, { useState, useCallback } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  Alert,
  RefreshControl,
} from 'react-native';
import { useRouter, useFocusEffect } from 'expo-router';
import * as DocumentPicker from 'expo-document-picker';
import * as FileSystem from 'expo-file-system/legacy';
import { Ionicons } from '@expo/vector-icons';
import DocumentCard from '../../components/DocumentCard';
import EmptyState from '../../components/EmptyState';

const DOCS_DIR = FileSystem.documentDirectory + 'pdfs/';

interface Document {
  name: string;
  uri: string;
  size: number;
  modifiedAt: number;
}

export default function DocumentsScreen() {
  const router = useRouter();
  const [documents, setDocuments] = useState<Document[]>([]);
  const [refreshing, setRefreshing] = useState(false);

  const loadDocuments = async () => {
    try {
      const dirInfo = await FileSystem.getInfoAsync(DOCS_DIR);
      if (!dirInfo.exists) {
        await FileSystem.makeDirectoryAsync(DOCS_DIR, { intermediates: true });
      }
      const files = await FileSystem.readDirectoryAsync(DOCS_DIR);
      const pdfFiles = files.filter((f) => f.toLowerCase().endsWith('.pdf'));
      const docs: Document[] = await Promise.all(
        pdfFiles.map(async (name) => {
          const info = await FileSystem.getInfoAsync(DOCS_DIR + name);
          return {
            name,
            uri: DOCS_DIR + name,
            size: (info as any).size || 0,
            modifiedAt: (info as any).modificationTime || Date.now(),
          };
        })
      );
      docs.sort((a, b) => b.modifiedAt - a.modifiedAt);
      setDocuments(docs);
    } catch (err) {
      console.error('Error loading documents:', err);
    }
  };

  useFocusEffect(
    useCallback(() => {
      loadDocuments();
    }, [])
  );

  const onRefresh = async () => {
    setRefreshing(true);
    await loadDocuments();
    setRefreshing(false);
  };

  const importPDF = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: 'application/pdf',
        copyToCacheDirectory: true,
      });
      if (!result.canceled && result.assets && result.assets.length > 0) {
        const asset = result.assets[0];
        const destUri = DOCS_DIR + asset.name;
        await FileSystem.copyAsync({ from: asset.uri, to: destUri });
        await loadDocuments();
        Alert.alert('Success', `"${asset.name}" has been imported.`);
      }
    } catch {
      Alert.alert('Error', 'Failed to import PDF.');
    }
  };

  const deleteDocument = (doc: Document) => {
    Alert.alert(
      'Delete Document',
      `Are you sure you want to delete "${doc.name}"?`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            await FileSystem.deleteAsync(doc.uri);
            await loadDocuments();
          },
        },
      ]
    );
  };

  const openDocument = (doc: Document) => {
    router.push({ pathname: '/viewer', params: { uri: doc.uri, name: doc.name } });
  };

  return (
    <View style={styles.container}>
      <FlatList
        data={documents}
        keyExtractor={(item) => item.uri}
        renderItem={({ item }) => (
          <DocumentCard
            document={item}
            onOpen={() => openDocument(item)}
            onDelete={() => deleteDocument(item)}
          />
        )}
        contentContainerStyle={
          documents.length === 0 ? styles.emptyContainer : styles.listContent
        }
        ListEmptyComponent={<EmptyState onImport={importPDF} />}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            tintColor="#4f46e5"
          />
        }
      />
      <TouchableOpacity style={styles.fab} onPress={importPDF}>
        <Ionicons name="add" size={28} color="#fff" />
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0f0f1a' },
  emptyContainer: { flex: 1 },
  listContent: { padding: 12 },
  fab: {
    position: 'absolute',
    right: 20,
    bottom: 20,
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: '#4f46e5',
    alignItems: 'center',
    justifyContent: 'center',
    elevation: 6,
    shadowColor: '#4f46e5',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.4,
    shadowRadius: 8,
  },
});
