import assert from "node:assert/strict";
import { test } from "node:test";
import {
  CLICK_ATTACK_SEC,
  CLICK_LATENCY_BUDGET_SEC,
  CLICK_LATENCY_HINT,
  CLICK_LOOKAHEAD_SEC,
  bindClickContextFactory,
  clickWithinBudget,
  playProceduralClick,
  resetClickVoiceForTests,
  scheduleClick,
  type ClickAudio,
  type ClickFilter,
  type ClickGain,
  type ClickOscillator,
} from "../src/lib/click_voice.ts";
import { tickById } from "../src/lib/ticks.ts";

type Call = { name: string; value?: number; time?: number };

function param(calls: Call[], label: string) {
  return {
    value: 0,
    setValueAtTime(value: number, time: number) {
      calls.push({ name: `${label}.set`, value, time });
    },
    exponentialRampToValueAtTime(value: number, time: number) {
      calls.push({ name: `${label}.ramp`, value, time });
    },
  };
}

function fakeAudio(now = 1.25): { audio: ClickAudio; calls: Call[]; oscillators: number } {
  const calls: Call[] = [];
  let oscillators = 0;
  const audio: ClickAudio = {
    state: "running",
    currentTime: now,
    destination: { kind: "destination" },
    resume: async () => {
      calls.push({ name: "resume" });
    },
    createOscillator: () => {
      oscillators += 1;
      const osc: ClickOscillator = {
        type: "sine",
        frequency: param(calls, "frequency"),
        connect: () => calls.push({ name: "osc.connect" }),
        start: (time) => calls.push({ name: "start", time }),
        stop: (time) => calls.push({ name: "stop", time }),
      };
      return osc;
    },
    createGain: () => {
      const gain: ClickGain = {
        gain: param(calls, "gain"),
        connect: () => calls.push({ name: "gain.connect" }),
      };
      return gain;
    },
    createBiquadFilter: () => {
      const filter: ClickFilter = {
        type: "lowpass",
        frequency: { value: 0 },
        connect: () => undefined,
      };
      return filter;
    },
  };
  return { audio, calls, get oscillators() { return oscillators; } };
}

test("a procedural click is scheduled at the context clock with no lookahead", () => {
  assert.equal(CLICK_LOOKAHEAD_SEC, 0);
  assert.equal(CLICK_LATENCY_HINT, "interactive");
  assert.ok(CLICK_ATTACK_SEC <= CLICK_LATENCY_BUDGET_SEC);
  const house = tickById("click-01").recipe;
  const schedule = scheduleClick(4, house);
  assert.equal(schedule.start, 4);
  assert.equal(schedule.lookahead, 0);
  assert.ok(Math.abs(schedule.attackEnd - schedule.start - CLICK_ATTACK_SEC) < 1e-12);
  assert.ok(Math.abs(schedule.stop - schedule.start - (house.dur + 0.04)) < 1e-12);
  assert.equal(clickWithinBudget(schedule), true);
});

test("the house click starts once at currentTime on the interactive voice", () => {
  const { audio, calls } = fakeAudio(2);
  const schedule = playProceduralClick("click-01", audio);
  assert.ok(schedule);
  assert.equal(schedule.lookahead, 0);
  assert.equal(calls.filter((call) => call.name === "start").length, 1);
  assert.equal(calls.find((call) => call.name === "start")?.time, 2);
  assert.equal(calls.find((call) => call.name === "frequency.set")?.value, 1640);
  const attack = calls.find((call) => call.name === "gain.ramp");
  assert.equal(attack?.time, 2 + CLICK_ATTACK_SEC);
});

test("ten rapid previews retrigger ten oscillators and legacy ids use the house recipe", () => {
  const { audio, calls } = fakeAudio(0);
  for (let index = 0; index < 10; index += 1) playProceduralClick("chip", audio);
  assert.equal(calls.filter((call) => call.name === "start").length, 10);
  assert.equal(calls.filter((call) => call.name === "frequency.set" && call.value === 1640).length, 10);
  assert.ok(calls.filter((call) => call.name === "start").every((call) => call.time === 0));
});

test("a missing context does not throw and does not invent a start time", () => {
  resetClickVoiceForTests();
  bindClickContextFactory(() => null);
  assert.equal(playProceduralClick("click-02"), null);
  resetClickVoiceForTests();
});
