import React from "react";
import { AbsoluteFill, Img, interpolate, spring, staticFile, useCurrentFrame, useVideoConfig } from "remotion";
import { C, clamp, easeInOut, easeOut } from "../theme";
import { Backdrop, Card, Check, Dot, Headline, Kicker, Phone, Pop, Ring, SceneLayout, Sub } from "../ui";

/** Scene 7: history chart and streak. */
export const HistoryScene: React.FC = () => {
  const frame = useCurrentFrame();
  const days = [
    { d: "Mon", s: 38, r: 24 },
    { d: "Tue", s: 30, r: 18 },
    { d: "Wed", s: 26, r: 16 },
    { d: "Thu", s: 22, r: 12 },
    { d: "Fri", s: 18, r: 14 },
    { d: "Sat", s: 15, r: 9 },
    { d: "Sun", s: 12, r: 7 },
  ];
  const max = 70;
  const chartH = 520;
  const limitY = chartH * (1 - 50 / max);
  const streak = Math.round(interpolate(frame, [80, 115], [0, 6], { ...clamp, easing: easeOut }));
  return (
    <SceneLayout
      top={
        <>
          <Kicker>History</Kicker>
          <Headline text="Watch the habit shrink" highlight={["shrink"]} highlightColor={C.good} />
          <Sub>Daily totals, streaks and your busiest hour.</Sub>
        </>
      }
    >
      <div style={{ width: 880, display: "flex", flexDirection: "column", gap: 28 }}>
        <Pop delay={6}>
          <Card>
            <div style={{ display: "flex", gap: 30, fontSize: 30, fontWeight: 600, color: C.muted, marginBottom: 30 }}>
              <span style={{ display: "flex", alignItems: "center", gap: 12 }}>
                <Dot color={C.shorts} size={20} /> Shorts
              </span>
              <span style={{ display: "flex", alignItems: "center", gap: 12 }}>
                <Dot color={C.reels} size={20} /> Reels
              </span>
              <span style={{ marginLeft: "auto" }}>Limit 50</span>
            </div>
            <div style={{ position: "relative", height: chartH, display: "flex", alignItems: "flex-end", gap: 26 }}>
              <div
                style={{
                  position: "absolute",
                  left: 0,
                  right: 0,
                  top: limitY,
                  borderTop: "4px dashed rgba(255,255,255,0.45)",
                  opacity: interpolate(frame, [14, 24], [0, 1], clamp),
                }}
              />
              {days.map((d, i) => {
                const g = interpolate(frame, [16 + i * 6, 40 + i * 6], [0, 1], { ...clamp, easing: easeOut });
                return (
                  <div key={d.d} style={{ flex: 1, display: "flex", flexDirection: "column", justifyContent: "flex-end", height: "100%" }}>
                    <div
                      style={{
                        height: (d.r / max) * chartH * g,
                        backgroundColor: C.reels,
                        borderRadius: "14px 14px 0 0",
                        marginBottom: 4,
                      }}
                    />
                    <div style={{ height: (d.s / max) * chartH * g, backgroundColor: C.shorts, borderRadius: "0 0 6px 6px" }} />
                  </div>
                );
              })}
            </div>
            <div style={{ display: "flex", gap: 26, marginTop: 16 }}>
              {days.map((d) => (
                <div key={d.d} style={{ flex: 1, textAlign: "center", fontSize: 24, color: C.muted, fontWeight: 600 }}>
                  {d.d}
                </div>
              ))}
            </div>
          </Card>
        </Pop>
        <div style={{ display: "flex", gap: 28 }}>
          <Pop delay={76} style={{ flex: 1 }}>
            <Card>
              <div style={{ fontSize: 96, fontWeight: 800, color: C.good, lineHeight: 1 }}>{streak} 🔥</div>
              <div style={{ fontSize: 32, color: C.muted, marginTop: 12 }}>Day streak within limit</div>
            </Card>
          </Pop>
          <Pop delay={86} style={{ flex: 1 }}>
            <Card>
              <div style={{ fontSize: 96, fontWeight: 800, lineHeight: 1 }}>22:00</div>
              <div style={{ fontSize: 32, color: C.muted, marginTop: 12 }}>Busiest hour</div>
            </Card>
          </Pop>
        </div>
      </div>
    </SceneLayout>
  );
};

/** Scene 8: friction against weakening your own limit. */
export const FrictionScene: React.FC = () => {
  const frame = useCurrentFrame();
  const secs = Math.ceil(interpolate(frame, [20, 110], [30, 0], clamp));
  const items = ["Wait 30 seconds or type a phrase", "Raise the limit only once a day", "One 5-minute snooze, max"];
  return (
    <SceneLayout
      glowA={C.warning}
      top={
        <>
          <Kicker color={C.warning}>Self-control, built in</Kicker>
          <Headline text="No easy way out" highlight={["out"]} highlightColor={C.warning} />
        </>
      }
    >
      <div style={{ width: 860, display: "flex", flexDirection: "column", gap: 34, alignItems: "center" }}>
        <Pop delay={6} style={{ width: "100%" }}>
          <Card style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 26, padding: 56 }}>
            <div style={{ fontSize: 58, fontWeight: 800 }}>Are you sure?</div>
            <div style={{ fontSize: 34, color: C.muted, textAlign: "center", lineHeight: 1.35 }}>
              This change makes your limit weaker.
            </div>
            <Ring size={260} stroke={22} progress={secs / 30} color={C.warning}>
              <div style={{ fontSize: 96, fontWeight: 800, fontVariantNumeric: "tabular-nums" }}>{secs}</div>
              <div style={{ fontSize: 26, color: C.muted }}>seconds</div>
            </Ring>
          </Card>
        </Pop>
        <div style={{ display: "flex", flexDirection: "column", gap: 30, width: "100%" }}>
          {items.map((t, i) => (
            <Pop key={t} delay={40 + i * 12} from="-60px 0px">
              <div style={{ display: "flex", alignItems: "center", gap: 26, fontSize: 44, fontWeight: 600 }}>
                <Check size={58} color={C.warning} />
                {t}
              </div>
            </Pop>
          ))}
        </div>
      </div>
    </SceneLayout>
  );
};

/** Scene 9: widget, floating counter and health check. */
export const ExtrasScene: React.FC = () => {
  const frame = useCurrentFrame();
  const n = Math.round(interpolate(frame, [20, 70], [12, 19], { ...clamp, easing: easeInOut }));
  return (
    <SceneLayout
      top={
        <>
          <Kicker>Always in view</Kicker>
          <Headline text="Your count, everywhere" highlight={["everywhere"]} />
        </>
      }
    >
      <div style={{ width: 880, display: "flex", flexDirection: "column", gap: 34 }}>
        <Pop delay={8}>
          <Card style={{ display: "flex", alignItems: "center", gap: 44 }}>
            <div
              style={{
                width: 250,
                height: 250,
                borderRadius: 56,
                backgroundColor: "#24262B",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                flexShrink: 0,
              }}
            >
              <Ring size={200} stroke={18} progress={n / 50} color={C.shorts}>
                <div style={{ fontSize: 64, fontWeight: 800, lineHeight: 1 }}>{50 - n}</div>
                <div style={{ fontSize: 24, color: C.muted }}>left</div>
              </Ring>
            </div>
            <div>
              <div style={{ fontSize: 52, fontWeight: 800 }}>Home widget</div>
              <div style={{ fontSize: 36, color: C.muted, marginTop: 8 }}>See what's left at a glance.</div>
            </div>
          </Card>
        </Pop>
        <Pop delay={26}>
          <Card style={{ display: "flex", alignItems: "center", gap: 44 }}>
            <div style={{ width: 250, display: "flex", justifyContent: "center", flexShrink: 0 }}>
              <div
                style={{
                  backgroundColor: "rgba(0,0,0,0.85)",
                  border: `3px solid ${C.shorts}`,
                  borderRadius: 999,
                  padding: "18px 30px",
                  fontSize: 50,
                  fontWeight: 800,
                  fontVariantNumeric: "tabular-nums",
                  boxShadow: `0 0 40px ${C.shorts}66`,
                }}
              >
                {n}/50
              </div>
            </div>
            <div>
              <div style={{ fontSize: 52, fontWeight: 800 }}>Live counter</div>
              <div style={{ fontSize: 36, color: C.muted, marginTop: 8 }}>A small badge while you scroll.</div>
            </div>
          </Card>
        </Pop>
        <Pop delay={44}>
          <Card style={{ display: "flex", alignItems: "center", gap: 44 }}>
            <div style={{ width: 250, display: "flex", justifyContent: "center", flexShrink: 0 }}>
              <Check size={150} />
            </div>
            <div>
              <div style={{ fontSize: 52, fontWeight: 800 }}>Health check</div>
              <div style={{ fontSize: 36, color: C.muted, marginTop: 8 }}>Alerts you if counting stops.</div>
            </div>
          </Card>
        </Pop>
      </div>
    </SceneLayout>
  );
};

/** Scene 10: privacy. */
export const PrivacyScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const s = spring({ frame, fps, config: { damping: 12 } });
  const items = ["No account", "No ads", "Counts stay on your phone", "Anonymous stats, optional"];
  return (
    <Backdrop glowA={C.good} glowB={C.shorts}>
      <AbsoluteFill style={{ padding: "160px 90px", alignItems: "center", justifyContent: "center", gap: 50 }}>
        <div style={{ scale: String(interpolate(s, [0, 1], [0.3, 1])), opacity: interpolate(frame, [0, 6], [0, 1], clamp) }}>
          <svg width="260" height="260" viewBox="0 0 24 24">
            <path d="M12 2l8 3v6c0 5-3.4 9.4-8 11-4.6-1.6-8-6-8-11V5l8-3z" fill={C.good} opacity="0.18" stroke={C.good} strokeWidth="1.2" />
            <rect x="8.5" y="11" width="7" height="5.5" rx="1.2" fill={C.good} />
            <path d="M10 11V9.3a2 2 0 014 0V11" stroke={C.good} strokeWidth="1.4" fill="none" />
          </svg>
        </div>
        <Headline text="Private by design" highlight={["Private"]} highlightColor={C.good} delay={8} />
        <div style={{ display: "flex", flexDirection: "column", gap: 34, marginTop: 30 }}>
          {items.map((t, i) => (
            <Pop key={t} delay={28 + i * 10} from="0px 40px">
              <div style={{ display: "flex", alignItems: "center", gap: 28, fontSize: 54, fontWeight: 700 }}>
                <Check size={66} />
                {t}
              </div>
            </Pop>
          ))}
        </div>
        <Sub delay={80} size={40}>
          Never reads captions, usernames or messages.
        </Sub>
      </AbsoluteFill>
    </Backdrop>
  );
};

/** Scene 11: the real app screenshots fanned out. */
export const RealAppScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const shots = [
    { src: "history.jpg", rot: -10, x: -300, d: 14 },
    { src: "settings.jpg", rot: 10, x: 300, d: 20 },
    { src: "today.jpg", rot: 0, x: 0, d: 6 },
  ];
  return (
    <SceneLayout
      top={
        <>
          <Kicker>The real app</Kicker>
          <Headline text="Clean. Simple. Yours." />
        </>
      }
    >
      <div style={{ position: "relative", width: 900, height: 1100 }}>
        {shots.map((s) => {
          const sp = spring({ frame: frame - s.d, fps, config: { damping: 16 } });
          return (
            <div
              key={s.src}
              style={{
                position: "absolute",
                left: "50%",
                top: "50%",
                marginLeft: -240,
                marginTop: -500,
                opacity: interpolate(frame, [s.d, s.d + 8], [0, 1], clamp),
                translate: `${interpolate(sp, [0, 1], [0, s.x])}px ${interpolate(sp, [0, 1], [300, s.x === 0 ? 0 : 60])}px`,
                rotate: `${interpolate(sp, [0, 1], [0, s.rot])}deg`,
              }}
            >
              <Phone width={480}>
                <Img src={staticFile(s.src)} style={{ width: "100%", height: "100%", objectFit: "cover" }} />
              </Phone>
            </div>
          );
        })}
      </div>
    </SceneLayout>
  );
};

/** Scene 12: call to action. */
export const CtaScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const s = spring({ frame, fps, config: { damping: 14 } });
  return (
    <Backdrop>
      <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", gap: 46, padding: 90 }}>
        <Img
          src={staticFile("icon.svg")}
          style={{
            width: 300,
            height: 300,
            scale: String(interpolate(s, [0, 1], [0.5, 1])),
            opacity: interpolate(frame, [0, 8], [0, 1], clamp),
          }}
        />
        <Headline text="Scroll less. | Live more." highlight={["Live", "more."]} size={104} delay={8} />
        <Sub delay={22} size={50}>
          ReelGuard for Android
        </Sub>
        <Pop delay={34}>
          <div style={{ display: "flex", gap: 20 }}>
            {["Free", "Open source", "No ads"].map((t) => (
              <div
                key={t}
                style={{
                  border: `2px solid ${C.line}`,
                  backgroundColor: C.card,
                  borderRadius: 999,
                  padding: "16px 32px",
                  fontSize: 38,
                  fontWeight: 600,
                }}
              >
                {t}
              </div>
            ))}
          </div>
        </Pop>
        <Pop delay={48}>
          <div
            style={{
              marginTop: 30,
              backgroundColor: C.shorts,
              borderRadius: 999,
              padding: "34px 64px",
              fontSize: 50,
              fontWeight: 800,
              boxShadow: `0 20px 60px ${C.shorts}66`,
              scale: String(1 + 0.03 * Math.sin(Math.max(0, frame - 60) / 6)),
            }}
          >
            bilalansari.in/reelguard
          </div>
        </Pop>
        <div
          style={{
            fontSize: 30,
            color: C.muted,
            opacity: interpolate(frame, [70, 85], [0, 1], { ...clamp, easing: easeOut }),
          }}
        >
          Not affiliated with Instagram, Meta, YouTube or Google.
        </div>
      </AbsoluteFill>
    </Backdrop>
  );
};
