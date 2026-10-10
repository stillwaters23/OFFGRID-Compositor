<<<<<<< HEAD
import { useEffect, useRef, type RefObject } from "react";

type LaserStripProps = {
  hostRef: RefObject<HTMLDivElement | null>;
  bankRef: RefObject<HTMLDivElement | null>;
};

export function LaserStrip({ hostRef, bankRef }: LaserStripProps) {
  const stripRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const host = hostRef.current;
    const bank = bankRef.current;
    const strip = stripRef.current;
    if (!host || !bank || !strip) return;

    const update = () => {
      const barrels = bank.querySelectorAll<HTMLElement>(".barrel");
      const first = barrels.item(0);
      const last = barrels.item(barrels.length - 1);
      if (!first || !last) return;
      const hostBox = host.getBoundingClientRect();
      const firstBox = first.getBoundingClientRect();
      const lastBox = last.getBoundingClientRect();
      const left = Math.max(0, firstBox.left - hostBox.left - 16);
      const right = Math.min(hostBox.width, lastBox.right - hostBox.left + 16);
      strip.style.left = `${left}px`;
      strip.style.width = `${Math.max(0, right - left)}px`;
      strip.style.top = `${firstBox.top + firstBox.height / 2 - hostBox.top}px`;
    };

    update();
    const observer = typeof ResizeObserver === "undefined" ? null : new ResizeObserver(update);
    observer?.observe(host);
    observer?.observe(bank);
    bank.querySelectorAll(".barrel").forEach((barrel) => observer?.observe(barrel));
    window.addEventListener("resize", update);
    return () => {
      observer?.disconnect();
      window.removeEventListener("resize", update);
    };
  }, [bankRef, hostRef]);

  return <div ref={stripRef} className="laser-strip" data-laser-strip aria-hidden="true" />;
=======
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
>>>>>>> origin/copilot/auric-reel-look-settings
}
