import { SetEntry } from "../types/workout";

export type Suggestion = { weight: string; reps: string };
type Field = "weight" | "reps";

// The faint value shown in one field of set i.
function suggestField(
  entered: SetEntry[],
  lastSession: Suggestion[],
  i: number,
  field: Field,
): string {
  // 1. The value typed in the nearest set above.
  for (let above = i - 1; above >= 0; above--) {
    const value = entered[above][field];
    if (value.trim() !== "") return value;
  }

  // 2. The same set number last session.
  if (i < lastSession.length) return lastSession[i][field];

  // 3. Last session's last set.
  if (lastSession.length > 0) return lastSession[lastSession.length - 1][field];

  // 4. Nothing to suggest.
  return "";
}

export function suggestSets(
  entered: SetEntry[],
  lastSession: Suggestion[],
): Suggestion[] {
  return entered.map((_, i) => ({
    weight: suggestField(entered, lastSession, i, "weight"),
    reps: suggestField(entered, lastSession, i, "reps"),
  }));
}
