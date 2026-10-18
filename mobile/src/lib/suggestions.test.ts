import { fillFromSuggestions, suggestSets } from "./suggestions";

const empty = { reps: "", weight: "", completed: false };

describe("suggestSets", () => {
  it("suggests nothing when there is no previous session and nothing typed", () => {
    expect(suggestSets([empty, empty], [])).toEqual([
      { weight: "", reps: "" },
      { weight: "", reps: "" },
    ]);
  });
  it("suggests last session's sets when nothing is typed", () => {
    const lastSession = { weight: "20", reps: "8" };

    expect(
      suggestSets(
        [empty, empty, empty],
        [lastSession, lastSession, lastSession],
      ),
    ).toEqual([lastSession, lastSession, lastSession]);
  });

  it("carries a value typed in a set down to every set below it", () => {
    const lastSession = { weight: "20", reps: "8" };
    const typed = { weight: "25", reps: "6", completed: false };

    expect(
      suggestSets(
        [typed, empty, empty],
        [lastSession, lastSession, lastSession],
      ),
    ).toEqual([
      { weight: "20", reps: "8" },
      { weight: "25", reps: "6" },
      { weight: "25", reps: "6" },
    ]);
  });

  it("falls back for each field on its own", () => {
    const lastSession = { weight: "20", reps: "8" };
    const weightOnly = { weight: "25", reps: "", completed: false };

    expect(
      suggestSets(
        [weightOnly, empty, empty],
        [lastSession, lastSession, lastSession],
      ),
    ).toEqual([
      { weight: "20", reps: "8" },
      { weight: "25", reps: "8" },
      { weight: "25", reps: "8" },
    ]);
  });

  it("uses the same set number from last session, then its last set", () => {
    const lastSession = [
      { weight: "20", reps: "8" },
      { weight: "22", reps: "6" },
      { weight: "24", reps: "4" },
    ];

    expect(
      suggestSets([empty, empty, empty, empty, empty], lastSession),
    ).toEqual([
      { weight: "20", reps: "8" },
      { weight: "22", reps: "6" },
      { weight: "24", reps: "4" },
      { weight: "24", reps: "4" },
      { weight: "24", reps: "4" },
    ]);
  });

  it("follows the nearest typed set, not the first", () => {
    const set1 = { weight: "25", reps: "6", completed: false };
    const set2 = { weight: "27", reps: "5", completed: false };

    expect(
      suggestSets([set1, set2, empty], [{ weight: "20", reps: "8" }]),
    ).toEqual([
      { weight: "20", reps: "8" },
      { weight: "25", reps: "6" },
      { weight: "27", reps: "5" },
    ]);
  });
});

describe("fillFromSuggestions", () => {
  it("logs what was typed, or else the suggestion", () => {
    const lastSession = [{ weight: "20", reps: "8" }];
    const weightOnly = { weight: "25", reps: "", completed: true };

    expect(fillFromSuggestions([weightOnly], lastSession)).toEqual([
      { weight: "25", reps: "8", completed: true },
    ]);
  });

  it("doesn't carry down values filled in from a suggestion", () => {
    const pyramid = [
      { weight: "20", reps: "8" },
      { weight: "22", reps: "6" },
    ];
    // Set 1 was ticked without typing, so ticking filled in last session's 20 x 8.
    const filledByTick = {
      weight: "20",
      reps: "8",
      completed: true,
      fromSuggestion: true,
    };

    expect(suggestSets([filledByTick, empty], pyramid)).toEqual([
      { weight: "20", reps: "8" },
      { weight: "22", reps: "6" },
    ]);
  });
});
