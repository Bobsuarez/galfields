import { Redirect } from 'expo-router';

// Settings already lives at /settings (outside the tabs group). This route
// exists only to give it a tab bar entry.
export default function SettingsTabScreen() {
  return <Redirect href="/settings" />;
}
