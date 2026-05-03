import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ActivityIndicator,
  Share,
  Alert,
  Dimensions,
} from 'react-native';
import { useLocalSearchParams, useNavigation } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import Pdf from 'react-native-pdf';

const { width } = Dimensions.get('window');

export default function ViewerScreen() {
  const { uri, name } = useLocalSearchParams<{ uri: string; name: string }>();
  const navigation = useNavigation();
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [scale, setScale] = useState(1.0);

  React.useEffect(() => {
    const sharePDF = async () => {
      try {
        await Share.share({ url: uri as string, title: name as string });
      } catch {
        Alert.alert('Error', 'Failed to share this file.');
      }
    };

    navigation.setOptions({
      title: name || 'PDF Viewer',
      headerRight: () => (
        <TouchableOpacity onPress={sharePDF} style={{ marginRight: 4 }}>
          <Ionicons name="share-outline" size={22} color="#fff" />
        </TouchableOpacity>
      ),
    });
  }, [navigation, name, uri]);

  const source = { uri: uri as string, cache: true };

  return (
    <View style={styles.container}>
      {loading && (
        <View style={styles.loadingOverlay}>
          <ActivityIndicator size="large" color="#4f46e5" />
          <Text style={styles.loadingText}>Loading PDF...</Text>
        </View>
      )}
      <Pdf
        source={source}
        style={styles.pdf}
        trustAllCerts={false}
        onLoadComplete={(numberOfPages) => {
          setTotalPages(numberOfPages);
          setLoading(false);
        }}
        onPageChanged={(currentPage) => setPage(currentPage)}
        onError={(error) => {
          setLoading(false);
          Alert.alert('Error', 'Failed to load PDF. The file may be corrupted.');
          console.error(error);
        }}
        enablePaging={false}
        horizontal={false}
        enableAnnotationRendering={true}
        fitPolicy={0}
        scale={scale}
        minScale={0.5}
        maxScale={3.0}
      />
      <View style={styles.toolbar}>
        <TouchableOpacity
          onPress={() => setScale((s) => Math.max(0.5, s - 0.25))}
          style={styles.toolbarBtn}
        >
          <Ionicons name="remove" size={20} color="#fff" />
        </TouchableOpacity>
        <Text style={styles.pageInfo}>
          {totalPages > 0 ? `${page} / ${totalPages}` : '...'}
        </Text>
        <TouchableOpacity
          onPress={() => setScale((s) => Math.min(3.0, s + 0.25))}
          style={styles.toolbarBtn}
        >
          <Ionicons name="add" size={20} color="#fff" />
        </TouchableOpacity>
        <TouchableOpacity onPress={() => setScale(1.0)} style={styles.toolbarBtn}>
          <Ionicons name="contract" size={20} color="#aaa" />
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0f0f1a' },
  pdf: { flex: 1, width, backgroundColor: '#0f0f1a' },
  loadingOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: '#0f0f1a',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 10,
  },
  loadingText: { color: '#888', marginTop: 12, fontSize: 15 },
  toolbar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#1a1a2e',
    borderTopWidth: 1,
    borderTopColor: '#2d2d44',
    paddingVertical: 10,
    paddingHorizontal: 20,
    gap: 16,
  },
  toolbarBtn: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#2d2d44',
    alignItems: 'center',
    justifyContent: 'center',
  },
  pageInfo: {
    color: '#fff',
    fontSize: 15,
    fontWeight: '600',
    minWidth: 60,
    textAlign: 'center',
  },
});
