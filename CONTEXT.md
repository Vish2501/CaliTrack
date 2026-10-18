# CaliTrack

A workout tracker: users log the sets they do in each workout and see their progress over time.

## Language

### Logging

**Workout**:
A finished training session stored on the server: when it started, when it ended, and the sets done in it.
_Avoid_: Session, log

**Active workout**:
The workout a user is doing right now. It exists only on their phone until they finish it. A user has at most one.
_Avoid_: Draft, open workout, unfinished workout

**Discard**:
Throwing away an active workout. Nothing reaches the server.
_Avoid_: Cancel, delete

**Finish**:
The moment an active workout becomes a workout. The whole thing is saved at once, or nothing is saved.
_Avoid_: Commit, complete, end

**Set**:
One round of an exercise inside a workout, with its reps and weight.
_Avoid_: Entry, log

### Exercises

**Exercise**:
A movement a user can log sets against. Every exercise belongs to one user.
_Avoid_: Movement, lift

**Exercise type**:
What an exercise's sets record. It is one of:
- **Weight & reps**: weight lifted and reps (Squat).
- **Bodyweight**: reps only (Pull-up).
- **Weighted bodyweight**: weight added on top of bodyweight, and reps (Weighted pull-up).
- **Assisted bodyweight**: weight of assistance taken off bodyweight, and reps (Assisted pull-up).
- **Duration**: how long the position was held (Plank).

A variation that records different things, such as Pull-up and Weighted pull-up, is a separate exercise.
_Avoid_: Kind, mode, tracking type

**Muscle group**:
The one group an exercise trains: Pull, Push, Legs or Core. Every exercise has exactly one.
_Avoid_: Category, body part

**Archived exercise**:
An exercise the user has deleted. It no longer appears when choosing exercises, but its sets and history are kept. Creating an exercise with the same name restores it, so its history carries on.
_Avoid_: Deleted exercise

**Default exercises**:
The starter set of exercises each user is given once, when they first use the app.

### Progress

**PR**:
A user's best result on an exercise. What counts as best depends on the exercise type, for example heaviest weight for Weight & reps or longest hold for Duration.
_Avoid_: Record, max, best

**Volume**:
The weight × reps a user moved. Only weight that was lifted counts (Weight & reps, or the added weight of Weighted bodyweight). Bodyweight is never included, because CaliTrack doesn't know it.
_Avoid_: Tonnage, load

**Streak**:
The number of consecutive weeks with at least one workout, counting the current week if it already has one.
_Avoid_: Run, chain

### Time

**Week**:
Monday to Sunday in the user's own time zone.
