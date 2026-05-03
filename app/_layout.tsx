import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';

export default function RootLayout() {
  return (
    <>
      <Stack>
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        <Stack.Screen
          name="viewer"
          options={{
            title: 'PDF Viewer',
            headerStyle: { backgroundColor: '#1a1a2e' },
            headerTintColor: '#fff',
          }}
        />
      </Stack>
      <StatusBar style="light" />
    </>
  );
}
