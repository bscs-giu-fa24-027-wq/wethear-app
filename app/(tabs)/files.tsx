import React, { useState, useCallback } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  Alert,
  Share,
} from 'react-native';
import * as FileSystem from 'expo-file-system/legacy';
import { Ionicons } from '@expo/vector-icons';
import { useRouter, useFocusEffect } from 'expo-router';

const DOCS_DIR = FileSystem.documentDirectory + 'pdfs/';

interface FileItem {
  name: string;
  uri: string;
  size: number;
  modifiedAt: number;
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(timestamp: number): string {
  return new Date(timestamp).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

export default function FilesScreen() {
  const router = useRouter();
  const [files, setFiles] = useState<FileItem[]>([]);
  const [sortBy, setSortBy] = useState<'date' | 'name' | 'size'>('date');
  const [totalSize, setTotalSize] = useState(0);

  const sortFiles = (items: FileItem[], by: 'date' | 'name' | 'size'): FileItem[] => {
    return [...items].sort((a, b) => {
      if (by === 'name') return a.name.localeCompare(b.name);
      if (by === 'size') return b.size - a.size;
      return b.modifiedAt - a.modifiedAt;
    });
  };

  const loadFiles = async () => {
    try {
      const dirInfo = await FileSystem.getInfoAsync(DOCS_DIR);
      if (!dirInfo.exists) {
        await FileSystem.makeDirectoryAsync(DOCS_DIR, { intermediates: true });
      }
      const items = await FileSystem.readDirectoryAsync(DOCS_DIR);
      const pdfFiles = items.filter((f) => f.toLowerCase().endsWith('.pdf'));
      const fileItems: FileItem[] = await Promise.all(
        pdfFiles.map(async (name) => {
          const info = await FileSystem.getInfoAsync(DOCS_DIR + name);
          return {
            name,
            uri: DOCS_DIR + name,
            size: info.exists ? info.size : 0,
            // modificationTime is in seconds; multiply to milliseconds for consistency
            modifiedAt: info.exists ? info.modificationTime * 1000 : Date.now(),
          };
        })
      );
      const total = fileItems.reduce((sum, f) => sum + f.size, 0);
      setTotalSize(total);
      setFiles(sortFiles(fileItems, sortBy));
    } catch (err) {
      console.error(err);
    }
  };

  useFocusEffect(
    useCallback(() => {
      loadFiles();
    }, [sortBy])
  );

  const handleSort = (by: 'date' | 'name' | 'size') => {
    setSortBy(by);
    setFiles(sortFiles(files, by));
  };

  const shareFile = async (file: FileItem) => {
    try {
      await Share.share({ url: file.uri, title: file.name });
    } catch {
      Alert.alert('Error', 'Failed to share file.');
    }
  };

  const deleteFile = (file: FileItem) => {
    Alert.alert('Delete File', `Delete "${file.name}"?`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          await FileSystem.deleteAsync(file.uri);
          await loadFiles();
        },
      },
    ]);
  };

  const openFile = (file: FileItem) => {
    router.push({ pathname: '/viewer', params: { uri: file.uri, name: file.name } });
  };

  const renderFile = ({ item }: { item: FileItem }) => (
    <TouchableOpacity style={styles.fileRow} onPress={() => openFile(item)}>
      <View style={styles.fileIcon}>
        <Ionicons name="document-text" size={28} color="#ef4444" />
      </View>
      <View style={styles.fileInfo}>
        <Text style={styles.fileName} numberOfLines={1}>
          {item.name}
        </Text>
        <Text style={styles.fileMeta}>
          {formatSize(item.size)} · {formatDate(item.modifiedAt)}
        </Text>
      </View>
      <View style={styles.fileActions}>
        <TouchableOpacity
          onPress={() => shareFile(item)}
          style={styles.fileActionBtn}
        >
          <Ionicons name="share-outline" size={20} color="#4f46e5" />
        </TouchableOpacity>
        <TouchableOpacity
          onPress={() => deleteFile(item)}
          style={styles.fileActionBtn}
        >
          <Ionicons name="trash-outline" size={20} color="#ef4444" />
        </TouchableOpacity>
      </View>
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      <View style={styles.storageCard}>
        <Ionicons name="folder" size={32} color="#4f46e5" />
        <View style={{ marginLeft: 12 }}>
          <Text style={styles.storageTitle}>Storage</Text>
          <Text style={styles.storageText}>
            {files.length} PDF{files.length !== 1 ? 's' : ''} · {formatSize(totalSize)}{' '}
            used
          </Text>
        </View>
      </View>
      <View style={styles.sortRow}>
        <Text style={styles.sortLabel}>Sort by:</Text>
        {(['date', 'name', 'size'] as const).map((s) => (
          <TouchableOpacity
            key={s}
            style={[styles.sortBtn, sortBy === s && styles.sortBtnActive]}
            onPress={() => handleSort(s)}
          >
            <Text style={[styles.sortText, sortBy === s && styles.sortTextActive]}>
              {s.charAt(0).toUpperCase() + s.slice(1)}
            </Text>
          </TouchableOpacity>
        ))}
      </View>
      <FlatList
        data={files}
        keyExtractor={(item) => item.uri}
        renderItem={renderFile}
        contentContainerStyle={files.length === 0 ? styles.emptyContent : undefined}
        ListEmptyComponent={
          <View style={styles.empty}>
            <Ionicons name="folder-open-outline" size={64} color="#333" />
            <Text style={styles.emptyText}>No PDF files found</Text>
            <Text style={styles.emptySubtext}>Import PDFs from the Documents tab</Text>
          </View>
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0f0f1a' },
  storageCard: {
    flexDirection: 'row',
    alignItems: 'center',
    margin: 16,
    padding: 16,
    backgroundColor: '#1a1a2e',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#2d2d44',
  },
  storageTitle: { color: '#fff', fontWeight: '700', fontSize: 16 },
  storageText: { color: '#888', fontSize: 13, marginTop: 2 },
  sortRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    marginBottom: 8,
  },
  sortLabel: { color: '#888', marginRight: 8 },
  sortBtn: {
    paddingHorizontal: 12,
    paddingVertical: 5,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#333',
    marginRight: 6,
  },
  sortBtnActive: { backgroundColor: '#4f46e5', borderColor: '#4f46e5' },
  sortText: { color: '#888', fontSize: 13 },
  sortTextActive: { color: '#fff' },
  fileRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#1a1a2e',
  },
  fileIcon: {
    width: 44,
    height: 44,
    backgroundColor: '#1a1a2e',
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  fileInfo: { flex: 1, marginLeft: 12 },
  fileName: { color: '#fff', fontSize: 15, fontWeight: '500' },
  fileMeta: { color: '#666', fontSize: 12, marginTop: 2 },
  fileActions: { flexDirection: 'row', gap: 4 },
  fileActionBtn: { padding: 8 },
  emptyContent: { flex: 1 },
  empty: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingTop: 80,
  },
  emptyText: { color: '#555', fontSize: 18, fontWeight: '600', marginTop: 16 },
  emptySubtext: { color: '#444', fontSize: 14, marginTop: 6 },
});
