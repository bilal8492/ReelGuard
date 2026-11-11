import { Composition } from "remotion";
import { PlayFeature, PlayIcon } from "./PlayAssets";
import { ReelGuardPromo } from "./ReelGuardPromo";

// Scenes total 54.5 s; 11 transitions of 15 frames overlap → 1635 - 165 = 1470 frames (49 s).
export const RemotionRoot: React.FC = () => {
  return (
    <>
      <Composition
        id="ReelGuardPromo"
        component={ReelGuardPromo}
        durationInFrames={1470}
        fps={30}
        width={1080}
        height={1920}
      />
      {/* Stills for the Google Play listing (docs/PLAY_STORE.md): npx remotion still PlayIcon out.png */}
      <Composition id="PlayIcon" component={PlayIcon} durationInFrames={1} fps={30} width={512} height={512} />
      <Composition id="PlayFeature" component={PlayFeature} durationInFrames={1} fps={30} width={1024} height={500} />
    </>
  );
};
