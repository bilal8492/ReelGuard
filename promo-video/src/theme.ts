import { loadFont } from "@remotion/google-fonts/Inter";
import { Easing } from "remotion";

export const { fontFamily } = loadFont("normal", {
  weights: ["400", "500", "600", "700", "800"],
  subsets: ["latin"],
});

// Colours taken from the app's dark theme (ui/theme/Theme.kt) and website.
export const C = {
  bg: "#0B0C0F",
  surface: "#16181D",
  card: "#1C1E23",
  line: "rgba(255,255,255,0.10)",
  track: "#2C2E33",
  text: "#F2F3F5",
  muted: "#9AA1AD",
  shorts: "#3987E5",
  reels: "#F0703A",
  good: "#22C55E",
  warning: "#FAB219",
  critical: "#E66767",
  accent: "#7C9CFF",
} as const;

export const easeOut = Easing.bezier(0.16, 1, 0.3, 1);
export const easeInOut = Easing.bezier(0.65, 0, 0.35, 1);

export const clamp = {
  extrapolateLeft: "clamp",
  extrapolateRight: "clamp",
} as const;
