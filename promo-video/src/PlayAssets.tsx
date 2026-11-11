import { AbsoluteFill } from "remotion";
import { C, fontFamily } from "./theme";

/** The app icon as artwork: the same ring and play mark as icon.svg. */
const IconArt: React.FC<{ readonly size: number; readonly radius: number }> = ({ size, radius }) => (
  <svg width={size} height={size} viewBox="0 0 512 512" style={{ display: "block" }}>
    <defs>
      <linearGradient id="bg" x1="0.18" y1="0" x2="0.82" y2="1">
        <stop offset="0" stopColor="#22252B" />
        <stop offset="1" stopColor="#0B0C0F" />
      </linearGradient>
    </defs>
    <rect width="512" height="512" rx={radius} fill="url(#bg)" />
    <g transform="translate(256 256) scale(4.74) translate(-54 -54)" fill="none" strokeLinecap="round">
      <path d="M54,30 A24,24 0 1 1 54,78 A24,24 0 1 1 54,30" stroke="#3A3D44" strokeWidth="8" />
      <path d="M54,30 A24,24 0 0 1 62.21,76.55" stroke={C.shorts} strokeWidth="8" />
      <path d="M53.16,77.99 A24,24 0 0 1 31.45,62.21" stroke="#F0703A" strokeWidth="8" />
      <path d="M48,46 L48,62 L62,54 Z" fill="#FFFFFF" stroke="#FFFFFF" strokeWidth="4" strokeLinejoin="round" />
    </g>
  </svg>
);

/** Play Store app icon, 512x512. Full-bleed square: Google Play applies its own rounded mask. */
export const PlayIcon: React.FC = () => (
  <AbsoluteFill>
    <IconArt size={512} radius={0} />
  </AbsoluteFill>
);

/** Play Store feature graphic, 1024x500. */
export const PlayFeature: React.FC = () => (
  <AbsoluteFill
    style={{
      background: `radial-gradient(900px 500px at 78% 40%, rgba(124,156,255,0.16), transparent 60%), ${C.bg}`,
      fontFamily,
      color: C.text,
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "center",
      gap: 56,
    }}
  >
    <div style={{ borderRadius: 56, overflow: "hidden", boxShadow: "0 24px 60px rgba(0,0,0,0.5)", border: `1px solid ${C.line}` }}>
      <IconArt size={260} radius={0} />
    </div>
    <div style={{ display: "flex", flexDirection: "column", gap: 18 }}>
      <div style={{ fontSize: 92, fontWeight: 800, letterSpacing: -2, lineHeight: 1 }}>ReelGuard</div>
      <div style={{ fontSize: 36, fontWeight: 500, color: C.muted, lineHeight: 1.25 }}>
        Count your Reels and Shorts.
        <br />
        Stop at your daily limit.
      </div>
      <div style={{ display: "flex", gap: 28, fontSize: 28, fontWeight: 600, marginTop: 6 }}>
        <span style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <span style={{ width: 16, height: 16, borderRadius: 8, background: C.reels }} />
          Instagram Reels
        </span>
        <span style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <span style={{ width: 16, height: 16, borderRadius: 8, background: C.shorts }} />
          YouTube Shorts
        </span>
      </div>
    </div>
  </AbsoluteFill>
);
