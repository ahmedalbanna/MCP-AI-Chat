import React, { useRef, useState } from 'react';
import {
  View,
  Modal,
  StyleSheet,
  Text,
  TouchableOpacity,
  ActivityIndicator,
  Platform,
} from 'react-native';
import { WebView } from 'react-native-webview';
import { useTheme } from '../theme/ThemeContext';
import { saveWafCookies } from '../services/waf';

interface Props {
  visible: boolean;
  url: string;
  onCookiesReady: (cookies: Record<string, string>) => void;
  onDismiss: () => void;
}

const INJECTED_JS = `
  (function() {
    function getCookies() {
      var cookies = {};
      document.cookie.split(';').forEach(function(c) {
        var parts = c.trim().split('=');
        if (parts[0]) cookies[parts[0]] = parts.slice(1).join('=');
      });
      return cookies;
    }
    var cookies = getCookies();
    if (Object.keys(cookies).length > 0) {
      window.ReactNativeWebView.postMessage(JSON.stringify(cookies));
    }
  })();
  true;
`;

export function WafChallenge({ visible, url, onCookiesReady, onDismiss }: Props) {
  const { colors } = useTheme();
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState('Loading verification page...');
  const webViewRef = useRef<WebView>(null);

  const handleMessage = async (event: any) => {
    try {
      const cookies = JSON.parse(event.nativeEvent.data);
      if (Object.keys(cookies).length > 0) {
        const cookieStr = Object.entries(cookies)
          .map(([k, v]) => `${k}=${v}`)
          .join('; ');
        await saveWafCookies(cookieStr, new URL(url).hostname);
        setStatus('Verification complete!');
        setTimeout(() => onCookiesReady(cookies), 500);
      }
    } catch {}
  };

  const onNavigationStateChange = (navState: any) => {
    if (navState.url.includes('/v1/messages') && !navState.url.includes('aliyun')) {
      webViewRef.current?.injectJavaScript(INJECTED_JS);
    }
  };

  const onLoadEnd = () => {
    setLoading(false);
    webViewRef.current?.injectJavaScript(INJECTED_JS);
  };

  return (
    <Modal visible={visible} animationType="slide" presentationStyle="pageSheet">
      <View style={[styles.container, { backgroundColor: colors.background }]}>
        <View style={[styles.header, { backgroundColor: colors.surface, borderBottomColor: colors.border }]}>
          <View style={styles.headerLeft}>
            <View style={[styles.dot, { backgroundColor: colors.warning }]} />
            <Text style={[styles.headerTitle, { color: colors.textPrimary }]}>Verification Required</Text>
          </View>
          <TouchableOpacity onPress={onDismiss} style={styles.closeBtn}>
            <Text style={[styles.closeText, { color: colors.error }]}>Cancel</Text>
          </TouchableOpacity>
        </View>

        <Text style={[styles.instructions, { color: colors.textSecondary }]}>
          Complete the verification below to connect to the API. Your cookies will be saved for future requests.
        </Text>

        {loading && (
          <View style={styles.loadingOverlay}>
            <ActivityIndicator size="large" color={colors.primary} />
            <Text style={[styles.loadingText, { color: colors.textSecondary }]}>{status}</Text>
          </View>
        )}

        <WebView
          ref={webViewRef}
          source={{ uri: url }}
          style={styles.webview}
          onMessage={handleMessage}
          onLoadEnd={onLoadEnd}
          onNavigationStateChange={onNavigationStateChange}
          javaScriptEnabled
          domStorageEnabled
          sharedCookiesEnabled
          thirdPartyCookiesEnabled
          userAgent="Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1"
        />

        <View style={[styles.footer, { backgroundColor: colors.surface, borderTopColor: colors.border }]}>
          <Text style={[styles.footerText, { color: colors.textSecondary }]}>
            Solve the captcha, then the cookies will be captured automatically.
          </Text>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 14,
    borderBottomWidth: 1,
  },
  headerLeft: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  dot: { width: 8, height: 8, borderRadius: 4 },
  headerTitle: { fontSize: 16, fontWeight: '600' },
  closeBtn: { paddingHorizontal: 8, paddingVertical: 4 },
  closeText: { fontSize: 15, fontWeight: '600' },
  instructions: {
    fontSize: 13,
    paddingHorizontal: 16,
    paddingVertical: 10,
    lineHeight: 18,
  },
  webview: { flex: 1 },
  loadingOverlay: {
    ...StyleSheet.absoluteFill,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(255,255,255,0.9)',
    zIndex: 10,
  },
  loadingText: { marginTop: 10, fontSize: 13 },
  footer: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderTopWidth: 1,
  },
  footerText: { fontSize: 12, textAlign: 'center' },
});
