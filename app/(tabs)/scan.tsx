import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Alert,
  Image,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import * as FileSystem from 'expo-file-system/legacy';

const DOCS_DIR = FileSystem.documentDirectory + 'pdfs/';

interface ScannedPage {
  uri: string;
  id: string;
}

export default function ScanScreen() {
  const [scannedPages, setScannedPages] = useState<ScannedPage[]>([]);
  const [filter, setFilter] = useState<'original' | 'grayscale' | 'bw'>('original');

  const captureDocument = async () => {
    const { status } = await ImagePicker.requestCameraPermissionsAsync();
    if (status !== 'granted') {
      Alert.alert(
        'Permission Required',
        'Camera permission is needed to scan documents.'
      );
      return;
    }
    const result = await ImagePicker.launchCameraAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      quality: 1,
      allowsEditing: true,
      aspect: [3, 4],
    });
    if (!result.canceled && result.assets.length > 0) {
      const page: ScannedPage = {
        uri: result.assets[0].uri,
        id: Date.now().toString(),
      };
      setScannedPages((prev) => [...prev, page]);
    }
  };

  const pickFromGallery = async () => {
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      quality: 1,
      allowsMultipleSelection: true,
    });
    if (!result.canceled && result.assets.length > 0) {
      const newPages: ScannedPage[] = result.assets.map((asset) => ({
        uri: asset.uri,
        id: Date.now().toString() + Math.random(),
      }));
      setScannedPages((prev) => [...prev, ...newPages]);
    }
  };

  const removePage = (id: string) => {
    setScannedPages((prev) => prev.filter((p) => p.id !== id));
  };

  const saveAsPDF = async () => {
    if (scannedPages.length === 0) {
      Alert.alert('No Pages', 'Please scan or add at least one page first.');
      return;
    }
    // Ensure the pdfs directory exists
    const dirInfo = await FileSystem.getInfoAsync(DOCS_DIR);
    if (!dirInfo.exists) {
      await FileSystem.makeDirectoryAsync(DOCS_DIR, { intermediates: true });
    }
    Alert.alert(
      'Save Document',
      'Document scan saved! In a production app, these images would be converted to PDF using a library like react-native-html-to-pdf or a backend service.',
      [{ text: 'OK', onPress: () => setScannedPages([]) }]
    );
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Document Scanner</Text>
        <Text style={styles.headerSubtitle}>
          {scannedPages.length === 0
            ? 'Capture or import pages to create a PDF'
            : `${scannedPages.length} page${scannedPages.length > 1 ? 's' : ''} captured`}
        </Text>
      </View>

      {scannedPages.length > 0 && (
        <>
          <View style={styles.filterRow}>
            <Text style={styles.filterLabel}>Filter:</Text>
            {(['original', 'grayscale', 'bw'] as const).map((f) => (
              <TouchableOpacity
                key={f}
                style={[styles.filterBtn, filter === f && styles.filterBtnActive]}
                onPress={() => setFilter(f)}
              >
                <Text
                  style={[styles.filterText, filter === f && styles.filterTextActive]}
                >
                  {f === 'bw' ? 'B&W' : f.charAt(0).toUpperCase() + f.slice(1)}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
          <ScrollView
            horizontal
            style={styles.pagesScroll}
            contentContainerStyle={styles.pagesContent}
          >
            {scannedPages.map((page, index) => (
              <View key={page.id} style={styles.pageContainer}>
                <Image
                  source={{ uri: page.uri }}
                  style={[
                    styles.pageImage,
                    filter === 'grayscale' && styles.grayscale,
                  ]}
                />
                <Text style={styles.pageNumber}>Page {index + 1}</Text>
                <TouchableOpacity
                  style={styles.removeBtn}
                  onPress={() => removePage(page.id)}
                >
                  <Ionicons name="close-circle" size={22} color="#ef4444" />
                </TouchableOpacity>
              </View>
            ))}
          </ScrollView>
        </>
      )}

      <View style={styles.actionsContainer}>
        <TouchableOpacity style={styles.actionBtn} onPress={captureDocument}>
          <Ionicons name="camera" size={28} color="#fff" />
          <Text style={styles.actionBtnText}>Camera</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.actionBtn, styles.actionBtnSecondary]}
          onPress={pickFromGallery}
        >
          <Ionicons name="images" size={28} color="#4f46e5" />
          <Text style={[styles.actionBtnText, styles.actionBtnTextSecondary]}>
            Gallery
          </Text>
        </TouchableOpacity>
      </View>

      {scannedPages.length > 0 && (
        <TouchableOpacity style={styles.saveBtn} onPress={saveAsPDF}>
          <Ionicons name="save" size={22} color="#fff" />
          <Text style={styles.saveBtnText}>
            Save as PDF ({scannedPages.length} pages)
          </Text>
        </TouchableOpacity>
      )}

      <View style={styles.tipsContainer}>
        <Text style={styles.tipsTitle}>📋 Scanning Tips</Text>
        <Text style={styles.tipItem}>
          • Place document on a flat, contrasting surface
        </Text>
        <Text style={styles.tipItem}>
          • Ensure good lighting to avoid shadows
        </Text>
        <Text style={styles.tipItem}>
          • Hold the camera directly above the document
        </Text>
        <Text style={styles.tipItem}>
          • Use "Allow Editing" to crop and straighten
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0f0f1a' },
  header: { padding: 20, paddingBottom: 12 },
  headerTitle: { fontSize: 22, fontWeight: 'bold', color: '#fff' },
  headerSubtitle: { fontSize: 14, color: '#888', marginTop: 4 },
  filterRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    marginBottom: 8,
  },
  filterLabel: { color: '#888', marginRight: 8 },
  filterBtn: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: '#333',
    marginRight: 8,
  },
  filterBtnActive: { backgroundColor: '#4f46e5', borderColor: '#4f46e5' },
  filterText: { color: '#888', fontSize: 13 },
  filterTextActive: { color: '#fff' },
  pagesScroll: { maxHeight: 200, marginBottom: 12 },
  pagesContent: { paddingHorizontal: 12 },
  pageContainer: { width: 130, marginRight: 12, alignItems: 'center' },
  pageImage: {
    width: 120,
    height: 160,
    borderRadius: 8,
    borderWidth: 2,
    borderColor: '#333',
  },
  grayscale: { opacity: 0.7 },
  pageNumber: { color: '#aaa', fontSize: 12, marginTop: 4 },
  removeBtn: { position: 'absolute', top: -6, right: -6 },
  actionsContainer: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    gap: 12,
    marginTop: 8,
  },
  actionBtn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#4f46e5',
    borderRadius: 12,
    paddingVertical: 16,
    gap: 8,
  },
  actionBtnSecondary: {
    backgroundColor: 'transparent',
    borderWidth: 2,
    borderColor: '#4f46e5',
  },
  actionBtnText: { color: '#fff', fontSize: 16, fontWeight: '600' },
  actionBtnTextSecondary: { color: '#4f46e5' },
  saveBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#059669',
    borderRadius: 12,
    paddingVertical: 14,
    marginHorizontal: 16,
    marginTop: 12,
    gap: 8,
  },
  saveBtnText: { color: '#fff', fontSize: 16, fontWeight: '600' },
  tipsContainer: {
    margin: 16,
    padding: 16,
    backgroundColor: '#1a1a2e',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#2d2d44',
  },
  tipsTitle: { color: '#fff', fontWeight: '600', marginBottom: 8, fontSize: 15 },
  tipItem: { color: '#aaa', fontSize: 13, marginBottom: 4 },
});
