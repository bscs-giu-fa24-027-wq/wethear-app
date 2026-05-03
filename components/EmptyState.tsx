import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';

interface Props {
  onImport: () => void;
}

export default function EmptyState({ onImport }: Props) {
  return (
    <View style={styles.container}>
      <Ionicons name="document-text-outline" size={80} color="#2d2d44" />
      <Text style={styles.title}>No Documents Yet</Text>
      <Text style={styles.subtitle}>
        Import a PDF to get started or use the scanner to create one
      </Text>
      <TouchableOpacity style={styles.button} onPress={onImport}>
        <Ionicons name="cloud-upload-outline" size={20} color="#fff" />
        <Text style={styles.buttonText}>Import PDF</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 40,
    paddingTop: 40,
  },
  title: {
    color: '#fff',
    fontSize: 22,
    fontWeight: '700',
    marginTop: 20,
    textAlign: 'center',
  },
  subtitle: {
    color: '#555',
    fontSize: 15,
    textAlign: 'center',
    marginTop: 10,
    lineHeight: 22,
  },
  button: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: '#4f46e5',
    paddingHorizontal: 24,
    paddingVertical: 14,
    borderRadius: 12,
    marginTop: 30,
  },
  buttonText: { color: '#fff', fontSize: 16, fontWeight: '600' },
});
