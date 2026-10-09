import { createFileRoute } from "@tanstack/react-router";
import { AuricApp } from "@/components/auric/app";
import { AuricBoundary } from "@/components/auric/error-boundary";
import { BootRitual } from "@/components/auric/boot-ritual";

export const Route = createFileRoute("/")({ component: Home });

function Home() {
  return (
    <AuricBoundary>
      <BootRitual>
        <AuricApp />
      </BootRitual>
    </AuricBoundary>
  );
}
