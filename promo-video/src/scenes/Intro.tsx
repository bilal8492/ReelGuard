import React from "react";
import { AbsoluteFill, Easing, Img, interpolate, spring, staticFile, useCurrentFrame, useVideoConfig } from "remotion";
import { C, clamp, easeOut } from "../theme";
import { Backdrop, Headline, Phone, Sub } from "../ui";

const CARD_COLORS = [
  ["#FF5F6D", "#FFC371"],
  ["#4776E6", "#8E54E9"],
  ["#11998E", "#38EF7D"],
  ["#F7971E", "#FFD200"],
  ["#EC008C", "#FC6767"],
  ["#00C6FF", "#0072FF"],
];

/** Scene 1: the problem — endless swiping, counter racing up. */
export const HookScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { durationInFrames } = useVideoConfig();
  const phoneW = 560;
  const screenH = phoneW * 2.08 * 0.95;
  // Accelerating scroll: position measured in "reels".
  const reels = interpolate(frame, [0, durationInFrames], [0, 87], {
    ...clamp,
    easing: Easing.bezier(0.55, 0, 0.85, 0.4),
  });
  const count = Math.floor(reels) + 1;
  return (
    <Backdrop glowA={C.reels} glowB="#8E54E9">
      <AbsoluteFill style={{ padding: "170px 90px", alignItems: "center" }}>
        <Headline text="One more reel…" size={104} />
        <div style={{ height: 70 }} />
        <div style={{ position: "relative" }}>
          <Phone width={phoneW}>
            {Array.from({ length: 4 }).map((_, k) => {
              const idx = Math.floor(reels) + k;
              const y = (idx - reels) * screenH;
              const [a, b] = CARD_COLORS[idx % CARD_COLORS.length];
              return (
                <div
                  key={idx}
                  style={{
                    position: "absolute",
                    left: 0,
                    right: 0,
                    top: y,
                    height: screenH,
                    background: `linear-gradient(160deg, ${a}, ${b})`,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  <svg width="140" height="140" viewBox="0 0 24 24" style={{ opacity: 0.85 }}>
                    <path d="M8 5v14l11-7z" fill="#fff" />
                  </svg>
                  <div style={{ position: "absolute", left: 40, bottom: 70, right: 40 }}>
                    <div style={{ width: "55%", height: 22, borderRadius: 11, background: "rgba(255,255,255,0.75)" }} />
                    <div style={{ width: "80%", height: 18, borderRadius: 9, background: "rgba(255,255,255,0.45)", marginTop: 16 }} />
                  </div>
                </div>
              );
            })}
          </Phone>
          {/* Swipe count badge */}
          <div
            style={{
              position: "absolute",
              right: -60,
              top: 120,
              backgroundColor: C.text,
              color: C.bg,
              borderRadius: 999,
              padding: "18px 34px",
              fontSize: 54,
              fontWeight: 800,
              fontVariantNumeric: "tabular-nums",
              boxShadow: "0 20px 50px rgba(0,0,0,0.5)",
              rotate: "6deg",
            }}
          >
            #{count}
          </div>
        </div>
        <div style={{ height: 60 }} />
        <Sub delay={durationInFrames - 50}>…said {count >= 80 ? 87 : count} reels ago.</Sub>
      </AbsoluteFill>
    </Backdrop>
  );
};

/** Scene 2: brand reveal. */
export const BrandScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const s = spring({ frame, fps, config: { damping: 14, stiffness: 110 } });
  return (
    <Backdrop>
      <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", gap: 50 }}>
        <Img
          src={staticFile("icon.svg")}
          style={{
            width: 380,
            height: 380,
            scale: String(interpolate(s, [0, 1], [0.4, 1])),
            rotate: `${interpolate(s, [0, 1], [-25, 0])}deg`,
            opacity: interpolate(frame, [0, 8], [0, 1], clamp),
            filter: "drop-shadow(0 40px 80px rgba(57,135,229,0.35))",
          }}
        />
        <div
          style={{
            fontSize: 150,
            fontWeight: 800,
            letterSpacing: -5,
            opacity: interpolate(frame, [10, 22], [0, 1], clamp),
            translate: interpolate(frame, [10, 30], ["0px 40px", "0px 0px"], { ...clamp, easing: easeOut }),
          }}
        >
          Reel<span style={{ color: C.shorts }}>Guard</span>
        </div>
        <Sub delay={24} size={52}>
          Take back your scroll.
        </Sub>
      </AbsoluteFill>
    </Backdrop>
  );
};
