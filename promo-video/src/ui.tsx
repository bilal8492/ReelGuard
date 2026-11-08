import React from "react";
import {
  AbsoluteFill,
  Interactive,
  interpolate,
  spring,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { C, clamp, easeOut, fontFamily } from "./theme";

/** Dark backdrop with two slowly drifting glows in the Shorts blue and Reels orange. */
export const Backdrop: React.FC<{
  readonly glowA?: string;
  readonly glowB?: string;
  readonly children?: React.ReactNode;
}> = ({ glowA = C.shorts, glowB = C.reels, children }) => {
  const frame = useCurrentFrame();
  const t = frame / 30;
  return (
    <AbsoluteFill style={{ backgroundColor: C.bg, fontFamily, color: C.text, overflow: "hidden" }}>
      <div
        style={{
          position: "absolute",
          width: 1300,
          height: 1300,
          left: -420 + Math.sin(t * 0.6) * 60,
          top: -380 + Math.cos(t * 0.5) * 50,
          borderRadius: "50%",
          background: `radial-gradient(circle, ${glowA}55 0%, ${glowA}00 62%)`,
        }}
      />
      <div
        style={{
          position: "absolute",
          width: 1300,
          height: 1300,
          right: -480 + Math.cos(t * 0.55) * 60,
          bottom: -420 + Math.sin(t * 0.45) * 60,
          borderRadius: "50%",
          background: `radial-gradient(circle, ${glowB}44 0%, ${glowB}00 62%)`,
        }}
      />
      {children}
    </AbsoluteFill>
  );
};

/** Small uppercase label above a headline. */
export const Kicker: React.FC<{ readonly color?: string; readonly delay?: number; readonly children: React.ReactNode }> = ({
  color = C.accent,
  delay = 0,
  children,
}) => {
  const frame = useCurrentFrame();
  return (
    <div
      style={{
        fontSize: 34,
        fontWeight: 700,
        letterSpacing: 6,
        textTransform: "uppercase",
        color,
        opacity: interpolate(frame, [delay, delay + 12], [0, 1], clamp),
        translate: interpolate(frame, [delay, delay + 18], ["0px 20px", "0px 0px"], { ...clamp, easing: easeOut }),
      }}
    >
      {children}
    </div>
  );
};

/** Headline that rises in word by word. */
export const Headline: React.FC<{
  readonly text: string;
  readonly delay?: number;
  readonly size?: number;
  readonly align?: "left" | "center";
  readonly highlight?: string[];
  readonly highlightColor?: string;
}> = ({ text, delay = 4, size = 96, align = "center", highlight = [], highlightColor = C.shorts }) => {
  const frame = useCurrentFrame();
  const words = text.split(" ");
  return (
    <Interactive.Div
      name="Headline"
      style={{
        fontSize: size,
        fontWeight: 800,
        lineHeight: 1.05,
        letterSpacing: -2,
        textAlign: align,
        display: "flex",
        flexWrap: "wrap",
        justifyContent: align === "center" ? "center" : "flex-start",
        columnGap: size * 0.26,
      }}
    >
      {words.map((w, i) => {
        if (w === "|") return <div key={i} style={{ flexBasis: "100%", height: 0 }} />;
        const start = delay + i * 3;
        return (
          <span
            key={i}
            style={{
              display: "inline-block",
              color: highlight.includes(w) ? highlightColor : C.text,
              opacity: interpolate(frame, [start, start + 10], [0, 1], clamp),
              translate: interpolate(frame, [start, start + 18], ["0px 50px", "0px 0px"], { ...clamp, easing: easeOut }),
            }}
          >
            {w}
          </span>
        );
      })}
    </Interactive.Div>
  );
};

/** Body copy that fades up. */
export const Sub: React.FC<{ readonly delay?: number; readonly children: React.ReactNode; readonly size?: number }> = ({
  delay = 14,
  children,
  size = 46,
}) => {
  const frame = useCurrentFrame();
  return (
    <div
      style={{
        fontSize: size,
        fontWeight: 500,
        color: C.muted,
        textAlign: "center",
        lineHeight: 1.3,
        opacity: interpolate(frame, [delay, delay + 14], [0, 1], clamp),
        translate: interpolate(frame, [delay, delay + 20], ["0px 24px", "0px 0px"], { ...clamp, easing: easeOut }),
      }}
    >
      {children}
    </div>
  );
};

/** Springs a child in (scale + fade) after `delay` frames. */
export const Pop: React.FC<{
  readonly delay?: number;
  readonly from?: string;
  readonly children: React.ReactNode;
  readonly style?: React.CSSProperties;
}> = ({ delay = 0, from = "0px 60px", children, style }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const s = spring({ frame: frame - delay, fps, config: { damping: 16, stiffness: 120 } });
  return (
    <div
      style={{
        opacity: interpolate(frame, [delay, delay + 8], [0, 1], clamp),
        scale: String(interpolate(s, [0, 1], [0.85, 1])),
        translate: interpolate(s, [0, 1], [from, "0px 0px"]),
        ...style,
      }}
    >
      {children}
    </div>
  );
};

/** A phone outline that holds an app screen. */
export const Phone: React.FC<{
  readonly width?: number;
  readonly children: React.ReactNode;
  readonly style?: React.CSSProperties;
}> = ({ width = 640, children, style }) => {
  const height = width * 2.08;
  return (
    <div
      style={{
        width,
        height,
        borderRadius: width * 0.13,
        padding: width * 0.025,
        background: "linear-gradient(160deg, #3A3D44, #15171B 40%, #2A2D33)",
        boxShadow: "0 60px 120px rgba(0,0,0,0.6), 0 0 0 2px rgba(255,255,255,0.06)",
        position: "relative",
        ...style,
      }}
    >
      <div
        style={{
          width: "100%",
          height: "100%",
          borderRadius: width * 0.11,
          overflow: "hidden",
          backgroundColor: "#0A0A0A",
          position: "relative",
        }}
      >
        {children}
        <div
          style={{
            position: "absolute",
            top: width * 0.03,
            left: "50%",
            width: width * 0.05,
            height: width * 0.05,
            marginLeft: -width * 0.025,
            borderRadius: "50%",
            backgroundColor: "#000",
            boxShadow: "0 0 0 3px #1a1a1a",
          }}
        />
      </div>
    </div>
  );
};

/** Circular progress ring like the app's Today screen. `progress` 0..1. */
export const Ring: React.FC<{
  readonly size: number;
  readonly stroke: number;
  readonly progress: number;
  readonly color: string;
  readonly track?: string;
  readonly children?: React.ReactNode;
}> = ({ size, stroke, progress, color, track = C.track, children }) => {
  const r = (size - stroke) / 2;
  const circ = 2 * Math.PI * r;
  return (
    <div style={{ width: size, height: size, position: "relative" }}>
      <svg width={size} height={size} style={{ position: "absolute", inset: 0, rotate: "-90deg" }}>
        <circle cx={size / 2} cy={size / 2} r={r} stroke={track} strokeWidth={stroke} fill="none" />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={r}
          stroke={color}
          strokeWidth={stroke}
          fill="none"
          strokeLinecap="round"
          strokeDasharray={circ}
          strokeDashoffset={circ * (1 - Math.max(0.0001, Math.min(1, progress)))}
        />
      </svg>
      <div
        style={{
          position: "absolute",
          inset: 0,
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        {children}
      </div>
    </div>
  );
};

export const Card: React.FC<{ readonly children: React.ReactNode; readonly style?: React.CSSProperties }> = ({
  children,
  style,
}) => (
  <div
    style={{
      backgroundColor: C.card,
      border: `2px solid ${C.line}`,
      borderRadius: 44,
      padding: 44,
      ...style,
    }}
  >
    {children}
  </div>
);

export const Check: React.FC<{ readonly size?: number; readonly color?: string }> = ({ size = 56, color = C.good }) => (
  <svg width={size} height={size} viewBox="0 0 24 24">
    <circle cx="12" cy="12" r="12" fill={color} />
    <path d="M7 12.5l3.2 3.2L17 9" stroke="#fff" strokeWidth="2.6" fill="none" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

export const Dot: React.FC<{ readonly color: string; readonly size?: number }> = ({ color, size = 26 }) => (
  <div style={{ width: size, height: size, borderRadius: "50%", backgroundColor: color, flexShrink: 0 }} />
);

/** Standard scene layout: text block at top, visual centred below. */
export const SceneLayout: React.FC<{
  readonly top: React.ReactNode;
  readonly children: React.ReactNode;
  readonly glowA?: string;
  readonly glowB?: string;
}> = ({ top, children, glowA, glowB }) => (
  <Backdrop glowA={glowA} glowB={glowB}>
    <AbsoluteFill style={{ padding: "170px 90px 140px", display: "flex", flexDirection: "column", alignItems: "center" }}>
      <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 26 }}>{top}</div>
      <div style={{ flex: 1, width: "100%", display: "flex", alignItems: "center", justifyContent: "center" }}>
        {children}
      </div>
    </AbsoluteFill>
  </Backdrop>
);
