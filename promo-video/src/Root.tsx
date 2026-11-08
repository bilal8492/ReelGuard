import { Composition } from "remotion";
import { ReelGuardPromo } from "./ReelGuardPromo";

// Scenes total 54.5 s; 11 transitions of 15 frames overlap → 1635 - 165 = 1470 frames (49 s).
export const RemotionRoot: React.FC = () => {
  return (
    <Composition
      id="ReelGuardPromo"
      component={ReelGuardPromo}
      durationInFrames={1470}
      fps={30}
      width={1080}
      height={1920}
    />
  );
};
