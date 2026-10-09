import { useEffect, useState } from "react";
import { OrbToggle } from "@/components/auric/switch";
import { useAuric } from "@/lib/store";

type BatteryInfo = { level: string; charging: string };

export function DeviceRoom() {
  const batteryOn = useAuric((s) => s.deviceBatteryOn);
  const networkOn = useAuric((s) => s.deviceNetworkOn);
  const [battery, setBattery] = useState<BatteryInfo>({ level: "Unavailable", charging: "Unavailable" });
  const [ip, setIp] = useState("Hidden until you ask");
  const [threads, setThreads] = useState("Unavailable");

  useEffect(() => {
    setThreads(navigator.hardwareConcurrency ? String(navigator.hardwareConcurrency) : "Unavailable");
  }, []);

  useEffect(() => {
    if (!batteryOn) {
      setBattery({ level: "Off", charging: "Off" });
      return;
    }
    const nav = navigator as Navigator & { getBattery?: () => Promise<{ level: number; charging: boolean; addEventListener: (name: string, fn: () => void) => void; removeEventListener: (name: string, fn: () => void) => void }> };
    if (!nav.getBattery) {
      setBattery({ level: "Unavailable", charging: "Unavailable" });
      return;
    }
    let mounted = true;
    void nav.getBattery().then((b) => {
      if (!mounted) return;
      const read = () => setBattery({
        level: `${Math.round(b.level * 100)}%`,
        charging: b.charging ? "Charging" : "Not charging",
      });
      read();
      b.addEventListener("levelchange", read);
      b.addEventListener("chargingchange", read);
    }).catch(() => setBattery({ level: "Unavailable", charging: "Unavailable" }));
    return () => { mounted = false; };
  }, [batteryOn]);

  async function revealIp() {
    setIp("Looking up…");
    try {
      const res = await fetch("https://api.ipify.org?format=json");
      const data = await res.json() as { ip?: string };
      setIp(data.ip || "Unavailable");
    } catch {
      setIp("Unavailable");
    }
  }

  return (
    <div className="tx-device">
      <p className="tx-muted">Unsupported readings stay Unavailable. None of this drives the clock.</p>
      <p className="tx-prompt">~ $ auric device --battery</p>
      <div className="tx-toggle">
        <span className="tx-core-name">battery</span>
        <OrbToggle
          on={batteryOn}
          onChange={(v) => useAuric.getState().setDeviceBatteryOn(v)}
          label="battery"
        />
      </div>
      <p className="tx-ink">level: {batteryOn ? battery.level : "Off"}</p>
      <p className="tx-ink">charging: {batteryOn ? battery.charging : "Off"}</p>
      <p className="tx-prompt">~ $ nproc</p>
      <p className="tx-ink">logical threads: {threads}</p>
      <p className="tx-muted">This is not CPU or GPU load. Browsers do not expose that here.</p>
      <p className="tx-prompt">~ $ auric device --net</p>
      <div className="tx-toggle">
        <span className="tx-core-name">network</span>
        <OrbToggle
          on={networkOn}
          onChange={(v) => useAuric.getState().setDeviceNetworkOn(v)}
          label="network"
        />
      </div>
      <p className="tx-ink">public ip: {networkOn ? ip : "Hidden until you ask"}</p>
      {networkOn ? (
        <button type="button" className="tx-btn tx-btn-min" onClick={() => void revealIp()}>
          Reveal public IP
        </button>
      ) : null}
      <p className="tx-muted">This optional lookup leaves the device. It is not used for time, zone, or alarms.</p>
    </div>
  );
}
