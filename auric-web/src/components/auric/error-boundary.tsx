import { Component, type ReactNode } from "react";
import { PitButton } from "@/components/auric/pit-button";

type Props = { children: ReactNode };
type State = { err: string | null };

export class AuricBoundary extends Component<Props, State> {
  state: State = { err: null };

  static getDerivedStateFromError(err: unknown): State {
    return { err: err instanceof Error ? err.message : "The stone slipped." };
  }

  render() {
    if (!this.state.err) return this.props.children;
    return (
      <div className="grid min-h-dvh place-items-center bg-felt px-6 text-center text-digit">
        <div>
          <p className="font-display text-[11px] tracking-[0.4em] text-accent">AURIC</p>
          <h1 className="mt-3 font-display text-2xl">The cut slipped</h1>
          <p className="mt-2 text-sm text-muted">Something interrupted AURIC. Try opening it again.</p>
          <details className="mt-3 max-w-sm break-words text-sm text-muted"><summary>Details</summary>{this.state.err}</details>
          <PitButton className="mt-6 magnet" onClick={() => this.setState({ err: null })}>
            Enter
          </PitButton>
        </div>
      </div>
    );
  }
}
