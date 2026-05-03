import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';

interface Document {
  name: string;
  uri: string;
  size: number;
  modifiedAt: number;
}

interface Props {
  document: Document;
  onOpen: () => void;
  onDelete: () => void;
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(timestamp: number): string {
  const d = new Date(timestamp);
  return d.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

export default function DocumentCard({ document, onOpen, onDelete }: Props) {
  return (
    <TouchableOpacity style={styles.card} onPress={onOpen} activeOpacity={0.8}>
      <View style={styles.iconContainer}>
        <Ionicons name="document-text" size={36} color="#ef4444" />
        <Text style={styles.pdfLabel}>PDF</Text>
      </View>
      <View style={styles.content}>
        <Text style={styles.name} numberOfLines={2}>
          {document.name}
        </Text>
        <Text style={styles.meta}>
          {formatSize(document.size)} · {formatDate(document.modifiedAt)}
        </Text>
      </View>
      <View style={styles.actions}>
        <TouchableOpacity style={styles.actionBtn} onPress={onOpen}>
          <Ionicons name="eye-outline" size={22} color="#4f46e5" />
        </TouchableOpacity>
        <TouchableOpacity style={styles.actionBtn} onPress={onDelete}>
          <Ionicons name="trash-outline" size={22} color="#ef4444" />
        </TouchableOpacity>
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#1a1a2e',
    borderRadius: 12,
    padding: 12,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: '#2d2d44',
  },
  iconContainer: { alignItems: 'center', width: 56 },
  pdfLabel: { color: '#ef4444', fontSize: 10, fontWeight: '700', marginTop: 2 },
  content: { flex: 1, marginHorizontal: 12 },
  name: { color: '#fff', fontSize: 15, fontWeight: '600' },
  meta: { color: '#666', fontSize: 12, marginTop: 4 },
  actions: { flexDirection: 'row', gap: 4 },
  actionBtn: { padding: 8 },
});
