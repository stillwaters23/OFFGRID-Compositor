import assert from "node:assert/strict";
import test from "node:test";
import { reelFeel } from "../src/lib/audio.ts";
import { useAuric } from "../src/lib/store.ts";
import { HOUSE_CLICK_ID, verifyTicks } from "../src/lib/ticks.ts";

test("reelFeel plays the house click once and sends the existing vibration pattern", () => {
  const originalWindow = Object.getOwnPropertyDescriptor(globalThis, "window");
  const originalNavigator = Object.getOwnPropertyDescriptor(globalThis, "navigator");
  const originalState = useAuric.getState();
  const frequencies: number[] = [];
  const vibrations: (number | number[])[] = [];

  class MockAudioContext {
    state = "running";
    currentTime = 0;
    destination = {};
    createGain() {
      return {
        gain: {
          setValueAtTime() {},
          exponentialRampToValueAtTime() {},
        },
        connect() {},
      };
    }
    createOscillator() {
      return {
        type: "sine",
        frequency: { setValueAtTime: (frequency: number) => frequencies.push(frequency) },
        connect() {},
        start() {},
        stop() {},
      };
    }
  }

  try {
    Object.defineProperty(globalThis, "window", {
      configurable: true,
      value: { AudioContext: MockAudioContext },
    });

    Object.defineProperty(globalThis, "navigator", {
      configurable: true,
      value: { vibrate: (pattern: number | number[]) => vibrations.push(pattern) },
    });
    useAuric.setState({ soundEnabled: true, reelSound: HOUSE_CLICK_ID });

    reelFeel();

    assert.deepEqual(frequencies, [1640]);
    assert.deepEqual(vibrations, [[12, 18, 42]]);
  } finally {
    useAuric.setState({
      soundEnabled: originalState.soundEnabled,
      reelSound: originalState.reelSound,
    });
    if (originalWindow) Object.defineProperty(globalThis, "window", originalWindow);
    else Reflect.deleteProperty(globalThis, "window");
    if (originalNavigator) Object.defineProperty(globalThis, "navigator", originalNavigator);
    else Reflect.deleteProperty(globalThis, "navigator");
  }
});

test("the shipped tick recipes remain valid", () => {
  assert.deepEqual(verifyTicks(), []);
});
