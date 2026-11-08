import React from "react";
import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { C, clamp, easeInOut, easeOut } from "../theme";
import { Card, Check, Dot, Headline, Kicker, Phone, Pop, Ring, SceneLayout, Sub } from "../ui";

/** Recreation of the app's Today screen, driven by numbers so it can animate. */
const TodayScreen: React.FC<{
  readonly shorts: number;
  readonly reels: number;
  readonly limit: number;
  readonly color: string;
  readonly status: string;
  readonly statusColor: string;
}> = ({ shorts, reels, limit, color, status, statusColor }) => {
  const total = shorts + reels;
  return (
    <div style={{ padding: "70px 30px 30px", display: "flex", flexDirection: "column", gap: 22 }}>
      <div style={{ fontSize: 44, fontWeight: 800 }}>Today</div>
      <div
        style={{
          backgroundColor: C.card,
          border: `2px solid ${C.line}`,
          borderRadius: 36,
          padding: 26,
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          gap: 22,
        }}
      >
        <Ring size={330} stroke={30} progress={total / limit} color={color}>
          <div style={{ fontSize: 100, fontWeight: 700, fontVariantNumeric: "tabular-nums", lineHeight: 1 }}>{total}</div>
          <div style={{ fontSize: 26, color: C.muted, fontWeight: 600 }}>of {limit}</div>
        </Ring>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 12,
            backgroundColor: `${statusColor}22`,
            borderRadius: 999,
            padding: "10px 22px",
            fontSize: 24,
            fontWeight: 600,
          }}
        >
          <Dot color={statusColor} size={18} />
          {status}
        </div>
        <div style={{ display: "flex", justifyContent: "space-around", width: "100%" }}>
          {[
            [String(Math.max(0, limit - total)), "Remaining"],
            [`${Math.round(total * 0.47)} min`, "Est. time"],
            ["00:00", "Resets"],
          ].map(([v, l]) => (
            <div key={l} style={{ textAlign: "center" }}>
              <div style={{ fontSize: 30, fontWeight: 700 }}>{v}</div>
              <div style={{ fontSize: 20, color: C.muted }}>{l}</div>
            </div>
          ))}
        </div>
      </div>
      <div style={{ display: "flex", gap: 18 }}>
        {[
          ["Shorts", shorts, C.shorts],
          ["Reels", reels, C.reels],
        ].map(([name, n, col]) => (
          <div
            key={name as string}
            style={{
              flex: 1,
              backgroundColor: C.card,
              border: `2px solid ${C.line}`,
              borderRadius: 30,
              padding: 22,
            }}
          >
            <div style={{ display: "flex", alignItems: "center", gap: 10, fontSize: 24, fontWeight: 600 }}>
              <Dot color={col as string} size={16} />
              {name}
            </div>
            <div style={{ fontSize: 64, fontWeight: 700, fontVariantNumeric: "tabular-nums", margin: "8px 0" }}>{n}</div>
            <div style={{ height: 10, borderRadius: 5, backgroundColor: C.track }}>
              <div
                style={{
                  height: "100%",
                  width: `${Math.min(100, ((n as number) / limit) * 100)}%`,
                  borderRadius: 5,
                  backgroundColor: col as string,
                }}
              />
            </div>
          </div>
        ))}
      </div>
      <NavBar />
    </div>
  );
};

/** The app's floating bottom navigation pill. */
const NavBar: React.FC = () => (
  <div
    style={{
      position: "absolute",
      bottom: 46,
      left: "50%",
      marginLeft: -170,
      width: 340,
      height: 90,
      borderRadius: 999,
      backgroundColor: C.card,
      border: `2px solid ${C.line}`,
      display: "flex",
      alignItems: "center",
      padding: 8,
      gap: 10,
    }}
  >
    <div
      style={{
        flex: 1.6,
        height: "100%",
        borderRadius: 999,
        backgroundColor: C.shorts,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        fontSize: 24,
        fontWeight: 600,
        color: "#0B1A33",
      }}
    >
      Today
    </div>
    {[0, 1].map((i) => (
      <div key={i} style={{ flex: 1, display: "flex", justifyContent: "center" }}>
        <div style={{ width: 34, height: 26, borderRadius: 6, border: `4px solid ${C.muted}` }} />
      </div>
    ))}
  </div>
);

/** Scene 3: automatic counting. */
export const CountScene: React.FC = () => {
  const frame = useCurrentFrame();
  const p = interpolate(frame, [20, 110], [0, 1], { ...clamp, easing: easeInOut });
  return (
    <SceneLayout
      top={
        <>
          <Kicker>Automatic</Kicker>
          <Headline text="Counts every swipe" highlight={["swipe"]} />
          <Sub>Instagram Reels & YouTube Shorts, tracked per day.</Sub>
        </>
      }
    >
      <div style={{ position: "relative" }}>
        <Pop delay={6}>
          <Phone width={600}>
            <TodayScreen
              shorts={Math.round(12 * p)}
              reels={Math.round(7 * p)}
              limit={50}
              color={C.shorts}
              status="On track"
              statusColor={C.shorts}
            />
          </Phone>
        </Pop>
        <Pop delay={40} from="-120px 0px" style={{ position: "absolute", left: -80, bottom: 250 }}>
          <Chip color={C.shorts}>YouTube Shorts +1</Chip>
        </Pop>
        <Pop delay={62} from="120px 0px" style={{ position: "absolute", right: -80, bottom: 120 }}>
          <Chip color={C.reels}>Instagram Reels +1</Chip>
        </Pop>
      </div>
    </SceneLayout>
  );
};

const Chip: React.FC<{ readonly color: string; readonly children: React.ReactNode }> = ({ color, children }) => (
  <div
    style={{
      display: "flex",
      alignItems: "center",
      gap: 16,
      backgroundColor: "rgba(28,30,35,0.92)",
      border: `2px solid ${color}`,
      borderRadius: 999,
      padding: "20px 32px",
      fontSize: 38,
      fontWeight: 700,
      boxShadow: `0 20px 50px rgba(0,0,0,0.5), 0 0 40px ${color}55`,
      whiteSpace: "nowrap",
    }}
  >
    <Dot color={color} />
    {children}
  </div>
);

/** Scene 4: set your own rules. */
export const LimitScene: React.FC = () => {
  const frame = useCurrentFrame();
  const limit = Math.round(interpolate(frame, [18, 60], [100, 50], { ...clamp, easing: easeOut }));
  const warn = interpolate(frame, [50, 90], [0, 0.8], { ...clamp, easing: easeOut });
  const rowStyle: React.CSSProperties = {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    fontSize: 42,
    fontWeight: 600,
  };
  return (
    <SceneLayout
      top={
        <>
          <Kicker>Your rules</Kicker>
          <Headline text="Set a daily limit" highlight={["limit"]} />
          <Sub>Pick a number and a reset time. Add separate caps if you like.</Sub>
        </>
      }
    >
      <div style={{ width: 860, display: "flex", flexDirection: "column", gap: 30 }}>
        <Pop delay={8}>
          <Card style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <div style={{ fontSize: 42, fontWeight: 600 }}>Daily limit</div>
            <div
              style={{
                fontSize: 110,
                fontWeight: 800,
                color: C.shorts,
                fontVariantNumeric: "tabular-nums",
                lineHeight: 1,
              }}
            >
              {limit}
            </div>
          </Card>
        </Pop>
        <Pop delay={36}>
          <Card>
            <div style={rowStyle}>
              <span>Warn me at</span>
              <span style={{ color: C.warning, fontVariantNumeric: "tabular-nums" }}>{Math.round(warn * 100)}%</span>
            </div>
            <div style={{ height: 24, borderRadius: 12, backgroundColor: C.track, marginTop: 30 }}>
              <div
                style={{
                  height: "100%",
                  width: `${warn * 100}%`,
                  borderRadius: 12,
                  background: `linear-gradient(90deg, ${C.shorts}, ${C.warning})`,
                }}
              />
            </div>
          </Card>
        </Pop>
        <Pop delay={62}>
          <Card style={{ display: "flex", flexDirection: "column", gap: 34 }}>
            <div style={rowStyle}>
              <span>Resets at</span>
              <span style={{ color: C.muted }}>00:00</span>
            </div>
            <div style={rowStyle}>
              <span style={{ display: "flex", alignItems: "center", gap: 18 }}>
                <Dot color={C.shorts} /> Shorts cap
              </span>
              <span style={{ color: C.muted }}>30</span>
            </div>
            <div style={rowStyle}>
              <span style={{ display: "flex", alignItems: "center", gap: 18 }}>
                <Dot color={C.reels} /> Reels cap
              </span>
              <span style={{ color: C.muted }}>20</span>
            </div>
          </Card>
        </Pop>
      </div>
    </SceneLayout>
  );
};

/** Scene 5: warning notification, then the limit-reached screen. */
export const BlockScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const p = interpolate(frame, [0, 40, 75, 95], [19, 40, 40, 50], { ...clamp, easing: easeInOut });
  const total = Math.round(p);
  const notif = spring({ frame: frame - 34, fps, config: { damping: 15 } });
  const notifOut = interpolate(frame, [72, 82], [0, 1], clamp);
  const overlay = spring({ frame: frame - 98, fps, config: { damping: 18 } });
  const shorts = Math.round(total * 0.6);
  return (
    <SceneLayout
      glowA={C.critical}
      glowB={C.warning}
      top={
        <>
          <Kicker color={C.critical}>Hard stop</Kicker>
          <Headline text="Hit the limit? Done." highlight={["Done."]} highlightColor={C.critical} />
          <Sub>A heads-up as you get close, then a stop screen.</Sub>
        </>
      }
    >
      <Pop delay={0}>
        <Phone width={600}>
          <TodayScreen
            shorts={shorts}
            reels={total - shorts}
            limit={50}
            color={total >= 40 ? (total >= 50 ? C.critical : C.warning) : C.shorts}
            status={total >= 40 ? "Close to your limit" : "On track"}
            statusColor={total >= 40 ? C.warning : C.shorts}
          />
          {/* Warning notification */}
          <div
            style={{
              position: "absolute",
              left: 20,
              right: 20,
              top: 30,
              translate: `0px ${interpolate(notif, [0, 1], [-260, 0]) - notifOut * 260}px`,
              backgroundColor: "#2A2C31",
              borderRadius: 30,
              padding: "24px 26px",
              boxShadow: "0 20px 40px rgba(0,0,0,0.6)",
              display: "flex",
              gap: 18,
              alignItems: "center",
            }}
          >
            <div
              style={{
                width: 60,
                height: 60,
                borderRadius: 16,
                backgroundColor: C.warning,
                color: C.bg,
                fontSize: 40,
                fontWeight: 800,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                flexShrink: 0,
              }}
            >
              !
            </div>
            <div>
              <div style={{ fontSize: 26, fontWeight: 700 }}>Approaching your daily limit</div>
              <div style={{ fontSize: 22, color: C.muted, marginTop: 4 }}>40 of 50 Reels/Shorts watched today.</div>
            </div>
          </div>
          {/* Limit reached overlay */}
          <AbsoluteFill
            style={{
              backgroundColor: "#0A0A0C",
              opacity: interpolate(frame, [98, 108], [0, 1], clamp),
              alignItems: "center",
              justifyContent: "center",
              padding: 50,
              gap: 26,
            }}
          >
            <div style={{ scale: String(interpolate(overlay, [0, 1], [0.6, 1])) }}>
              <Ring size={220} stroke={22} progress={1} color={C.critical}>
                <svg width="90" height="90" viewBox="0 0 24 24">
                  <rect x="6" y="6" width="12" height="12" rx="2" fill={C.critical} />
                </svg>
              </Ring>
            </div>
            <div style={{ fontSize: 50, fontWeight: 800, textAlign: "center" }}>Daily limit reached</div>
            <div style={{ fontSize: 26, color: C.muted, textAlign: "center" }}>50 watched today (23 min)</div>
            <div style={{ height: 20 }} />
            <div
              style={{
                width: "100%",
                textAlign: "center",
                backgroundColor: C.shorts,
                borderRadius: 999,
                padding: "26px 0",
                fontSize: 30,
                fontWeight: 700,
              }}
            >
              Go to Home screen
            </div>
            <div
              style={{
                width: "100%",
                textAlign: "center",
                border: `2px solid ${C.line}`,
                borderRadius: 999,
                padding: "24px 0",
                fontSize: 28,
                fontWeight: 600,
                color: C.muted,
              }}
            >
              Snooze 5 minutes
            </div>
          </AbsoluteFill>
        </Phone>
      </Pop>
    </SceneLayout>
  );
};

/** Scene 6: only Reels & Shorts are limited. */
export const OnlyReelsScene: React.FC = () => {
  const free = ["Your feed", "Stories", "Messages", "Regular videos"];
  return (
    <SceneLayout
      glowA={C.good}
      top={
        <>
          <Kicker color={C.good}>Nothing else</Kicker>
          <Headline text="Only Reels & Shorts" highlight={["Reels", "Shorts"]} highlightColor={C.reels} />
          <Sub>Everything else on Instagram and YouTube keeps working.</Sub>
        </>
      }
    >
      <div style={{ width: 860, display: "flex", flexDirection: "column", gap: 26 }}>
        <div style={{ display: "flex", gap: 26 }}>
          {[
            ["Reels", C.reels],
            ["Shorts", C.shorts],
          ].map(([name, col], i) => (
            <Pop key={name} delay={8 + i * 6} style={{ flex: 1 }}>
              <Card
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 18,
                  fontSize: 46,
                  fontWeight: 700,
                  borderColor: col,
                  backgroundColor: `${col}1F`,
                }}
              >
                <Dot color={col} size={30} />
                {name}
                <span style={{ marginLeft: "auto", fontSize: 32, color: col }}>limited</span>
              </Card>
            </Pop>
          ))}
        </div>
        <Card style={{ display: "flex", flexDirection: "column", gap: 34 }}>
          {free.map((f, i) => (
            <Pop key={f} delay={32 + i * 9} from="-60px 0px">
              <div style={{ display: "flex", alignItems: "center", gap: 28, fontSize: 48, fontWeight: 600 }}>
                <Check size={62} />
                {f}
                <span style={{ marginLeft: "auto", fontSize: 32, color: C.good, fontWeight: 600 }}>never blocked</span>
              </div>
            </Pop>
          ))}
        </Card>
      </div>
    </SceneLayout>
  );
};
