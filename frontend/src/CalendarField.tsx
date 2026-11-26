import { useEffect, useRef, useState } from "react";
import flatpickr from "flatpickr";
import type { Instance } from "flatpickr/dist/types/instance";
import { CalendarDays } from "lucide-react";
import { isCalendarDay, localInput, normalizeClock } from "./shared";

interface Props {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  withTime?: boolean;
  invalid?: boolean;
  onDateBlur?: () => void;
}

export function CalendarField({
  id,
  label,
  value,
  onChange,
  withTime,
  invalid,
  onDateBlur,
}: Props) {
  const [day, clock = ""] = withTime ? value.split("T") : [value];
  const dateInput = useRef<HTMLInputElement>(null);
  const calendarButton = useRef<HTMLButtonElement>(null);
  const calendarInput = useRef<HTMLInputElement>(null);
  const picker = useRef<Instance | null>(null);
  const [open, setOpen] = useState(false);
  const changeDay = (next: string) =>
    onChange(withTime && (next || clock) ? next + "T" + clock : next);
  const onPick = useRef(changeDay);
  useEffect(() => {
    onPick.current = changeDay;
  });

  useEffect(() => {
    const input = dateInput.current!;
    const calendar = flatpickr(calendarInput.current!, {
      dateFormat: "Y-m-d",
      animate: false,
      locale: { ...flatpickr.l10ns.en, firstDayOfWeek: 1 },
      disableMobile: true,
      clickOpens: false,
      positionElement: input,
      ignoredFocusElements: [input, calendarButton.current!],
      now: localInput(new Date().toISOString()).slice(0, 10),
      onChange: (_, date) => onPick.current(date),
      onOpen: () => setOpen(true),
      onClose: (_, __, instance) => {
        setOpen(false);
        if (instance.calendarContainer.contains(document.activeElement))
          input.focus();
      },
    });
    calendar.calendarContainer.id = id + "-calendar";
    calendar.calendarContainer.setAttribute("role", "dialog");
    calendar.calendarContainer.setAttribute(
      "aria-label",
      "Calendar for " + label,
    );
    picker.current = calendar;
    return () => {
      calendar.destroy();
      picker.current = null;
    };
  }, [id, label]);

  useEffect(() => {
    if (!day) picker.current?.clear(false);
    else if (isCalendarDay(day)) picker.current?.setDate(day, false);
  }, [day]);

  function openCalendar() {
    const calendar = picker.current;
    if (!calendar) return;
    calendar.open();
    (
      calendar.selectedDateElem ||
      calendar.todayDateElem ||
      calendar.days.querySelector<HTMLElement>(".flatpickr-day")
    )?.focus();
  }

  return (
    <span className="calendar-field">
      <span className="calendar-date">
        <input
          ref={dateInput}
          id={id}
          aria-invalid={invalid}
          aria-describedby={invalid ? id + "-error" : undefined}
          placeholder="YYYY-MM-DD"
          autoComplete="off"
          maxLength={10}
          value={day}
          onChange={(e) => changeDay(e.target.value)}
          onBlur={onDateBlur}
          onKeyDown={(e) => {
            if (e.key === "ArrowDown") {
              e.preventDefault();
              openCalendar();
            }
          }}
        />
        <button
          ref={calendarButton}
          type="button"
          aria-label={"Choose date for " + label}
          aria-controls={id + "-calendar"}
          aria-haspopup="dialog"
          aria-expanded={open}
          onClick={() => (open ? picker.current?.close() : openCalendar())}
        >
          <CalendarDays size={17} />
        </button>
        <input ref={calendarInput} hidden aria-hidden="true" tabIndex={-1} />
      </span>
      {withTime && (
        <input
          className="calendar-clock"
          type="text"
          aria-label={label + " (24-hour time)"}
          aria-invalid={invalid}
          aria-describedby={invalid ? id + "-error" : undefined}
          placeholder="7 or 7:30"
          autoComplete="off"
          value={clock}
          onChange={(e) =>
            onChange(day || e.target.value ? day + "T" + e.target.value : "")
          }
          onBlur={() => {
            const normalized = normalizeClock(clock);
            if (normalized !== null && normalized !== clock) {
              onChange(day + "T" + normalized);
            }
          }}
        />
      )}
    </span>
  );
}
