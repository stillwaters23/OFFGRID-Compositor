import { createRootRoute, HeadContent, Outlet, Scripts } from "@tanstack/react-router";
import { AuthProvider } from "@/lib/auth/provider";
import { PreviewHostBridge } from "@/components/preview-host-bridge";
import { BRAND } from "@/lib/brand";
import appCss from "../styles.css?url";

const APP_NAME = "Auric";

export const Route = createRootRoute({
  head: () => ({
    meta: [
      { charSet: "utf-8" },
      { name: "viewport", content: "width=device-width, initial-scale=1, viewport-fit=cover, interactive-widget=resizes-content" },
      { title: APP_NAME },
      { name: "description", content: "Auric — Time, crowned. A Termux-native timer, stopwatch and alarm." },
      { name: "theme-color", content: "#000000" },
      { name: "apple-mobile-web-app-title", content: BRAND.name },
    ],
    links: [
      { rel: "icon", type: "image/png", sizes: "32x32", href: BRAND.favicon },
      { rel: "stylesheet", href: appCss },
      { rel: "manifest", href: "/__grok/manifest.webmanifest" },
      { rel: "apple-touch-icon", sizes: "180x180", href: BRAND.appleTouchIcon },
      {
        rel: "stylesheet",
        href: "https://fonts.googleapis.com/css2?family=Courier+Prime:wght@400;700&family=JetBrains+Mono:wght@400;500;700;800&family=UnifrakturCook:wght@700&family=Barlow+Condensed:wght@500;600;700&family=Cinzel:wght@500;600;700&family=Outfit:wght@300;400;500;600&display=swap",
      },
    ],
  }),
  component: () => (
    <html lang="en" suppressHydrationWarning className="antialiased">
      <head>
        <HeadContent />
      </head>
      <body>
        <PreviewHostBridge />
        <AuthProvider>
          <Outlet />
        </AuthProvider>
        <Scripts />
      </body>
    </html>
  ),
});
