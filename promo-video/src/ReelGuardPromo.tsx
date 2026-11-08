import React from "react";
import { springTiming, TransitionSeries } from "@remotion/transitions";
import { fade } from "@remotion/transitions/fade";
import { slide } from "@remotion/transitions/slide";
import { useVideoConfig } from "remotion";
import { BrandScene, HookScene } from "./scenes/Intro";
import { BlockScene, CountScene, LimitScene, OnlyReelsScene } from "./scenes/Features";
import { CtaScene, ExtrasScene, FrictionScene, HistoryScene, PrivacyScene, RealAppScene } from "./scenes/More";

// Each transition overlaps two scenes by this many frames.
export const TRANSITION_FRAMES = 15;

// Vertical "swipe up" between scenes, like moving to the next reel.
const swipe = () => slide({ direction: "from-bottom" });
const timing = () => springTiming({ config: { damping: 200 }, durationInFrames: TRANSITION_FRAMES });

export const ReelGuardPromo: React.FC = () => {
  const { fps } = useVideoConfig();
  return (
    <TransitionSeries>
      <TransitionSeries.Sequence name="Hook" durationInFrames={4 * fps}>
        <HookScene />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={fade()} timing={timing()} />
      <TransitionSeries.Sequence name="Brand" durationInFrames={3 * fps}>
        <BrandScene />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={swipe()} timing={timing()} />
      <TransitionSeries.Sequence name="Counts swipes" durationInFrames={5 * fps}>
        <CountScene />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={swipe()} timing={timing()} />
      <TransitionSeries.Sequence name="Daily limit" durationInFrames={5 * fps}>
        <LimitScene />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={swipe()} timing={timing()} />
      <TransitionSeries.Sequence name="Limit reached" durationInFrames={5.5 * fps}>
        <BlockScene />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={swipe()} timing={timing()} />
      <TransitionSeries.Sequence name="Only Reels & Shorts" durationInFrames={4.5 * fps}>
        <OnlyReelsScene />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={swipe()} timing={timing()} />
      <TransitionSeries.Sequence name="History" durationInFrames={5 * fps}>
        <HistoryScene />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={swipe()} timing={timing()} />
      <TransitionSeries.Sequence name="Friction" durationInFrames={4.5 * fps}>
        <FrictionScene />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={swipe()} timing={timing()} />
      <TransitionSeries.Sequence name="Widget & counter" durationInFrames={4.5 * fps}>
        <ExtrasScene />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={swipe()} timing={timing()} />
      <TransitionSeries.Sequence name="Privacy" durationInFrames={4.5 * fps}>
        <PrivacyScene />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={swipe()} timing={timing()} />
      <TransitionSeries.Sequence name="Real app" durationInFrames={4 * fps}>
        <RealAppScene />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={fade()} timing={timing()} />
      <TransitionSeries.Sequence name="Call to action" durationInFrames={5 * fps}>
        <CtaScene />
      </TransitionSeries.Sequence>
    </TransitionSeries>
  );
};
