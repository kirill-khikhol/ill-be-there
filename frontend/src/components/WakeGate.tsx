import { useEffect, useState, type ReactNode } from "react";
import { API_BASE, waitForHealth } from "../api";
import LangSwitch from "./LangSwitch";
import { useI18n } from "../i18n";

export default function WakeGate({ children }: { children: ReactNode }) {
  const { t, translateError } = useI18n();
  const [ready, setReady] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!API_BASE) {
      setReady(true);
      return;
    }
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
        <div className="wake-head">
          <h1>I'll Be There</h1>
          <LangSwitch compact />
        </div>
        <p>{t("wakeBody")}</p>
        <p className="hint">{t("wakeHint")}</p>
        <p>
          {error ? (
            <span className="error">{translateError(error, "errorWakeTimeout")}</span>
          ) : (
            t("wakeAttempt", { n: attempt })
          )}
        </p>
      </div>
    </div>
  );
}
