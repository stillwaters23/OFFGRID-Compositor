/** Settings witness for the laser row. Timer span reels mount their strip in Lane 1. */
export function LaserStrip() {
  return (
    <>
      <span className="reel-look-hairline" data-laser-hairline="" aria-hidden />
      <span className="reel-look-beam" data-laser-beam="" aria-hidden>
        <span className="reel-look-beam-shimmer">
          <span className="reel-look-beam-bloom" />
          <span className="reel-look-beam-core" />
        </span>
      </span>
    </>
  );
}
