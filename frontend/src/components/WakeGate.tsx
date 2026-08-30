import { useEffect, useState, type ReactNode } from "react";
import { waitForHealth } from "../api";

export default function WakeGate({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    waitForHealth((n) => {
      if (!cancelled) {
        setAttempt(n);
      }
    })
      .then(() => {
        if (!cancelled) {
          setReady(true);
        }
      })
      .catch((err: Error) => {
        if (!cancelled) {
          setError(err.message);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (ready) {
    return <>{children}</>;
  }

  return (
    <div className="wake">
      <div className="wake-card">
        <h1>I'll Be There</h1>
        <p>Сервер на бесплатном тарифе засыпает без трафика. Сейчас он просыпается.</p>
        <p className="hint">
          Обычно это занимает 1–2 минуты. После этого приложение будет тёплым минимум 15 минут.
        </p>
        <p>{error ? <span className="error">{error}</span> : `Попытка ${attempt}… проверяем /actuator/health`}</p>
      </div>
    </div>
  );
}
