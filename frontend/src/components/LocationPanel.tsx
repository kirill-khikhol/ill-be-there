import { useEffect, useMemo, useState } from "react";
import { api, loginHref } from "../api";
import { useAuth } from "../auth";
import type { DaySlots, Place, SlotDetails } from "../types";
import { CITY_LABEL } from "../types";

function todayInIsrael(): string {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Jerusalem",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date());
}

export default function LocationPanel({
  place,
  onClose,
  favorited,
  onToggleFavorite,
  onPromiseCreated,
}: {
  place: Place;
  onClose: () => void;
  favorited: boolean;
  onToggleFavorite: () => void;
  onPromiseCreated?: () => void;
}) {
  const { user, googleEnabled } = useAuth();
  const [date, setDate] = useState(todayInIsrael);
  const [day, setDay] = useState<DaySlots | null>(null);
  const [openSlot, setOpenSlot] = useState<string | null>(null);
  const [details, setDetails] = useState<SlotDetails | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [myIds, setMyIds] = useState<Record<string, number>>({});

  const load = async () => {
    setError(null);
    const slots = await api.daySlots(place.id, date);
    setDay(slots);
    if (user) {
      const mine = await api.myPromises();
      const map: Record<string, number> = {};
      mine
        .filter((item) => item.locationId === place.id && item.date === date)
        .forEach((item) => {
          map[item.slot] = item.id;
        });
      setMyIds(map);
    } else {
      setMyIds({});
    }
  };

  useEffect(() => {
    void load().catch((err: Error) => setError(err.message));
    setOpenSlot(null);
    setDetails(null);
  }, [place.id, date, user]);

  const visibleSlots = useMemo(() => {
    if (!day) {
      return [];
    }
    return day.slots;
  }, [day]);

  const toggleSlot = async (start: string) => {
    if (openSlot === start) {
      setOpenSlot(null);
      setDetails(null);
      return;
    }
    setOpenSlot(start);
    setDetails(await api.slotDetails(place.id, date, start));
  };

  const promise = async (slot: string) => {
    if (!user) {
      window.location.href = loginHref();
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const created = await api.createPromise(place.id, date, slot);
      await load();
      setDetails(await api.slotDetails(place.id, date, slot));
      onPromiseCreated?.();
      if (created.calendarWarning) {
        setError(created.calendarWarning);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось обещать");
    } finally {
      setBusy(false);
    }
  };

  const cancel = async (slot: string) => {
    const id = myIds[slot];
    if (!id) {
      return;
    }
    setBusy(true);
    try {
      await api.cancelPromise(id);
      await load();
      if (openSlot === slot) {
        setDetails(await api.slotDetails(place.id, date, slot));
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось отменить");
    } finally {
      setBusy(false);
    }
  };

  return (
    <aside className="panel">
      <div className="row">
        <div>
          <h2>{place.name}</h2>
          <div className="meta">
            {CITY_LABEL[place.city]} · спортплощадка
            {place.source === "USER" ? " · добавлена пользователем" : ""}
          </div>
        </div>
        <button className="ghost" onClick={onClose} type="button">
          Закрыть
        </button>
      </div>
      {user && (
        <button className={`ghost favorite-toggle ${favorited ? "active" : ""}`} type="button" onClick={onToggleFavorite}>
          {favorited ? "★ В избранном" : "☆ В избранное"}
        </button>
      )}
      <div className="date-row">
        <label htmlFor="promise-date">День</label>
        <input id="promise-date" type="date" value={date} onChange={(e) => setDate(e.target.value)} />
      </div>
      {error && <p className="error">{error}</p>}
      {day && visibleSlots.length === 0 && (
        <p className="hint">На этот день уже нет слотов после текущего времени.</p>
      )}
      <div className="slots">
        {visibleSlots.map((slot) => (
          <button
            key={slot.start}
            className={`slot ${slot.count === 0 ? "empty" : ""} ${openSlot === slot.start ? "open" : ""}`}
            type="button"
            onClick={() => void toggleSlot(slot.start)}
          >
            <strong>
              {slot.start}–{slot.end}
            </strong>
            <span>{slot.count === 0 ? "пока никого" : `${slot.count} обещали прийти`}</span>
            {myIds[slot.start] ? (
              <span
                className="ghost"
                onClick={(e) => {
                  e.stopPropagation();
                  void cancel(slot.start);
                }}
              >
                Отменить
              </span>
            ) : (
              <span
                className="btn"
                onClick={(e) => {
                  e.stopPropagation();
                  void promise(slot.start);
                }}
              >
                {user ? "Я буду" : "Войти и обещать"}
              </span>
            )}
            {openSlot === slot.start && details && (
              <div className="people">
                {details.people.length === 0 && <span className="hint">Список пуст</span>}
                {details.people.map((person, index) => (
                  <div className="person" key={`${person.name}-${index}`}>
                    {person.avatarUrl ? (
                      <img src={person.avatarUrl} alt="" />
                    ) : (
                      <div className="avatar-fallback" />
                    )}
                    <span>{person.name}</span>
                  </div>
                ))}
              </div>
            )}
          </button>
        ))}
      </div>
      {!user && googleEnabled && (
        <p className="hint">Чтобы обещание попало в Google Calendar, войдите через Google.</p>
      )}
      {user && user.hasCalendarAccess === false && (
        <p className="hint">
          Нет доступа к календарю.{" "}
          <a href={loginHref()}>Войдите снова</a> и разрешите Google Calendar.
        </p>
      )}
      {busy && <p className="hint">Сохраняем…</p>}
    </aside>
  );
}
