import { LOCALES, LOCALE_META } from "../i18n/messages";
import { useI18n } from "../i18n/I18nProvider";

export default function LangSwitch({ compact = false }: { compact?: boolean }) {
  const { locale, setLocale, t } = useI18n();
  return (
    <div className={`lang-switch ${compact ? "compact" : ""}`} role="group" aria-label={t("language")}>
      {LOCALES.map((code) => (
        <button
          key={code}
          type="button"
          className={locale === code ? "chip active" : "chip"}
          aria-pressed={locale === code}
          onClick={() => setLocale(code)}
        >
          {LOCALE_META[code].label}
        </button>
      ))}
    </div>
  );
}
