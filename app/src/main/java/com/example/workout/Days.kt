package com.example.workout

private fun work(vararg exercises: Exercise): List<Step> = exercises.map { Step.Work(it) }
private fun rest(label: String, seconds: Int): List<Step> = listOf(Step.Rest(label, seconds))

// ---------------------------------------------------------------------------------------------
// Chest and back (no equipment). Pull-ups and chin-ups are replaced with prone back work.
// ---------------------------------------------------------------------------------------------

private val standardPushUp = Exercise(
    "Standard Push-Ups", "Chest",
    "Hands just wider than your shoulders, body straight from head to heels. Lower your chest to the floor, then press back up. Max reps."
)
private val militaryPushUp = Exercise(
    "Military Push-Ups", "Chest",
    "Hands under your shoulders with your elbows tucked tight against your ribs. Lower your chest to the floor and press back up. Max reps."
)
private val wideFlyPushUp = Exercise(
    "Wide Fly Push-Ups", "Chest",
    "Hands well wider than your shoulders. Lower your chest between your hands, going as deep as you can, then press back up. Max reps."
)
private val declinePushUp = Exercise(
    "Decline Push-Ups", "Chest",
    "Feet up on a chair or couch, hands on the floor under your shoulders. Lower your chest to the floor and press up. Keep your body straight. Max reps."
)
private val diamondPushUp = Exercise(
    "Diamond Push-Ups", "Chest",
    "Hands close together under your chest, thumbs and index fingers touching in a diamond. Lower your chest to your hands and press up. Max reps."
)
private val diveBomberPushUp = Exercise(
    "Dive-Bomber Push-Ups", "Chest",
    "Start with your hips high in an upside-down V. Swoop your chest down and forward close to the floor, rise into a low upward dog, then push your hips back up to the V. Max reps."
)

private val proneY = Exercise(
    "Prone Y Pull-Down", "Back",
    "Replaces wide front pull-ups. Lie face down with your arms overhead in a Y. Lift your chest and arms off the floor, pull your elbows down toward your hips and squeeze your shoulder blades, then reach back out. Max reps.",
    videoQuery = "prone Y raise pull down exercise"
)
private val proneT = Exercise(
    "Prone T Squeeze", "Back",
    "Replaces closed grip overhand pull-ups. Lie face down with your arms straight out to the sides in a T. Lift your chest and arms, squeeze your shoulder blades together, then lower. Max reps.",
    videoQuery = "prone T raise exercise"
)
private val proneW = Exercise(
    "Prone W Pull-Back", "Back",
    "Replaces reverse grip chin-ups. Lie face down with your arms overhead. Lift your chest and pull your elbows back and down into a W by your ribs, squeezing your shoulder blades, then reach forward again. Max reps.",
    videoQuery = "prone W raise exercise"
)
private val proneYWSwitch = Exercise(
    "Prone Y/W Switch Pull", "Back",
    "Replaces switch grip pull-ups. Lie face down. Do 2 reps of the Y pull-down, then 2 reps of the W pull-back, and keep alternating. Max reps.",
    videoQuery = "prone Y W raise exercise"
)
private val halfSquatPulses = Exercise(
    "Half-Squat Pulses", "Back",
    "Replaces heavy pants. Feet shoulder-width apart, sit into a half squat with your hips back and chest up. Pulse up and down a few inches without standing all the way up.",
    videoQuery = "half squat pulses exercise"
)
private val supermanLifts = Exercise(
    "Alternating Superman Lifts", "Back",
    "Replaces lawnmowers. Lie face down with your arms overhead. Lift your right arm and left leg together, lower, then lift your left arm and right leg. Keep alternating.",
    videoQuery = "alternating superman exercise"
)
private val reverseSnowAngels = Exercise(
    "Reverse Snow Angels", "Back",
    "Replaces back flys. Lie face down with your arms at your sides and your chest slightly lifted. Sweep your arms in a wide arc overhead and back to your hips, keeping them off the floor.",
    videoQuery = "reverse snow angels exercise"
)

private val chestBackSteps =
    work(standardPushUp, proneY, militaryPushUp, proneW) + rest("Water break", 49) +
    work(wideFlyPushUp, proneT, declinePushUp, halfSquatPulses) + rest("Water break", 47) +
    work(diamondPushUp, supermanLifts, diveBomberPushUp, reverseSnowAngels) + rest("Water break", 64) +
    work(proneY, standardPushUp, proneW, militaryPushUp) + rest("Water break", 60) +
    work(proneT, wideFlyPushUp, halfSquatPulses, declinePushUp) + rest("Water break", 37) +
    work(supermanLifts, diamondPushUp, reverseSnowAngels, diveBomberPushUp) + rest("Cool down", 202)

// ---------------------------------------------------------------------------------------------
// Shoulders and arms (dumbbells).
// ---------------------------------------------------------------------------------------------

private val alternatingShoulderPress = Exercise(
    "Alternating Shoulder Presses", "Shoulders",
    "Dumbbells at shoulder height, palms forward. Press one overhead, lower it, then press the other. Keep your core tight.",
    weighted = true
)
private val inOutCurl = Exercise(
    "In & Out Bicep Curls", "Biceps",
    "Dumbbells at your sides. Curl up with your elbows pinned, alternating each rep between hands wide (out) and hands close to your body (in).",
    weighted = true
)
private val twoArmKickback = Exercise(
    "Two-Arm Triceps Kickback", "Triceps",
    "Hinge forward at the hips with your elbows pinned to your sides. Extend both dumbbells straight back until your arms are straight, squeeze, then bend back.",
    weighted = true
)
private val deepSwimmerPress = Exercise(
    "Deep Swimmer's Presses", "Shoulders",
    "A shoulder press with a long range of motion. Lower the dumbbells deeper than a regular press, then press overhead in a smooth arc.",
    weighted = true
)
private val concentrationCurl = Exercise(
    "Full Supination Concentration Curls", "Biceps",
    "Sit with one elbow braced against your inner thigh. Curl one dumbbell up while rotating your palm fully toward your shoulder, squeeze, then lower slowly. Alternate arms every rep: left, right, left, right. That is 2 reps on each arm.",
    weighted = true
)
private val chairDips = Exercise(
    "Chair Dips", "Triceps",
    "Hands on the edge of a sturdy chair behind you, legs out in front. Bend your elbows to lower your hips toward the floor, then press back up. Keep your back close to the chair."
)
private val uprightRows = Exercise(
    "Upright Rows", "Shoulders",
    "Dumbbells in front of your thighs. Pull them up to chest height with your elbows leading, then lower slowly.",
    weighted = true
)
private val staticArmCurl = Exercise(
    "Static Arm Curls", "Biceps",
    "Hold one dumbbell still with your elbow bent at 90 degrees while the other arm does full curls. Switch arms at the halfway call.",
    switchSides = true,
    weighted = true
)
private val flipGripKickback = Exercise(
    "Flip-Grip Twist Triceps Kickbacks", "Triceps",
    "Hinge forward with your elbows pinned. Kick the dumbbells straight back, flip your palms to face up at the top, then return and flip back.",
    weighted = true
)
private val seatedTwoAngleFly = Exercise(
    "Seated Two-Angle Shoulder Flys", "Shoulders",
    "Sit holding the dumbbells. Raise your arms out to the sides to shoulder height, then lean further forward and repeat. Alternate between the two angles.",
    weighted = true
)
private val cohenCurl = Exercise(
    "Crouching Cohen Curls", "Biceps",
    "Crouch low with your elbows resting against the inside of your knees. Curl the dumbbells up and lower slowly.",
    weighted = true
)
private val lyingTricepsExtension = Exercise(
    "Lying-Down Triceps Extensions", "Triceps",
    "Lie on your back with the dumbbells above your chest and your arms straight. Bend only your elbows to lower the weights toward your head, then extend back up.",
    weighted = true
)
private val inOutStraightArmFly = Exercise(
    "In & Out Straight-Arm Shoulder Flys", "Shoulders",
    "Keep your arms straight. Raise the dumbbells out to the sides to shoulder height, then alternate with raising them out in front of you.",
    weighted = true
)
private val congdonCurl = Exercise(
    "Congdon Curls", "Biceps",
    "A strict curl variation. Keep your elbows pinned, squeeze at the top and lower slowly with no swinging.",
    weighted = true
)
private val sideTriRise = Exercise(
    "Side Tri-Rises", "Triceps",
    "A one-arm triceps press done on your side. Do half the minute on each arm and switch at the halfway call.",
    switchSides = true,
    weighted = true
)

private val armsSteps =
    work(alternatingShoulderPress, inOutCurl, twoArmKickback, alternatingShoulderPress, inOutCurl, twoArmKickback) +
        rest("Ballistic stretch", 30) +
    work(deepSwimmerPress, concentrationCurl, chairDips, deepSwimmerPress, concentrationCurl, chairDips) +
        rest("Ballistic stretch", 30) +
    work(uprightRows, staticArmCurl, flipGripKickback, uprightRows, staticArmCurl, flipGripKickback) +
        rest("Ballistic stretch", 34) +
    work(seatedTwoAngleFly, cohenCurl, lyingTricepsExtension, seatedTwoAngleFly, cohenCurl, lyingTricepsExtension) +
        rest("Ballistic stretch", 38) +
    work(inOutStraightArmFly, congdonCurl, sideTriRise, inOutStraightArmFly, congdonCurl, sideTriRise) +
        rest("Cool down", 209)

// ---------------------------------------------------------------------------------------------
// Legs and back (no equipment). Weights removed, pull-ups replaced with prone back work.
// ---------------------------------------------------------------------------------------------

private val balancedLunge = Exercise(
    "Balanced Lunges", "Legs",
    "Step one foot back into a lunge and lower your back knee toward the floor. Drive back up and balance on the front leg for a beat before the next rep. Switch legs at the halfway call.",
    switchSides = true
)
private val calfRaiseSquat = Exercise(
    "Calf-Raise Squats", "Legs",
    "Feet shoulder-width apart. Squat until your thighs are about parallel, stand up, and rise onto your toes at the top. Repeat."
)
private val superSkaters = Exercise(
    "Super Skaters", "Legs",
    "Start on one foot with a soft knee. Sweep the other leg back behind you in a speed-skating position and swing your arms for balance. This is not a leap sideways. Switch feet at the halfway call.",
    switchSides = true
)
private val wallSquat = Exercise(
    "Wall Squats", "Legs",
    "Back flat against a wall, slide down until your thighs are parallel to the floor with your knees over your ankles. Hold for the whole minute."
)
private val stepBackLunge = Exercise(
    "Step Back Lunge", "Legs",
    "Stand tall, step one foot back and lower into a lunge, then push back to standing. Alternate legs each rep."
)
private val alternatingSideLunge = Exercise(
    "Alternating Side Lunge", "Legs",
    "Take a big step out to the side and sit your hips back over that leg, keeping the other leg straight. Push back to center. Alternate sides each rep."
)
private val singleLegWallSquat = Exercise(
    "Single-Leg Wall Squat", "Legs",
    "Back against a wall in a wall sit, then lift one foot off the floor and hold. Switch legs at the halfway call.",
    switchSides = true
)
private val deadliftSquat = Exercise(
    "Deadlift Squats", "Legs",
    "On one leg, hinge forward at the hips and lower your chest while the other leg extends behind you, then return to standing. Switch legs at the halfway call.",
    switchSides = true,
    videoQuery = "deadlift squat bodyweight exercise"
)
private val threeWayLunge = Exercise(
    "Three-Way Lunge", "Legs",
    "Lunge to the side and return, lunge on a diagonal and return, then lunge straight forward and return. That is one rep. Nothing goes behind you. Alternate legs."
)
private val sneakyLunge = Exercise(
    "Sneaky Lunge", "Legs",
    "Step one foot back and across behind your front leg into a diagonal lunge, lower, then return. Alternate legs.",
    videoQuery = "sneaky lunge exercise p90x"
)
private val chairSalutations = Exercise(
    "Chair Salutations", "Legs",
    "Hold a deep chair pose: hips back, thighs near parallel, weight in your heels. Stay low for the whole minute.",
    videoQuery = "chair salutations exercise p90x"
)
private val toeRollIsoLunge = Exercise(
    "Toe-Roll Iso Lunge", "Legs",
    "Hold a low lunge. Roll the back heel up onto your toes and back down, keeping your front knee over your ankle. Switch legs at the halfway call.",
    switchSides = true,
    videoQuery = "toe roll iso lunge p90x"
)
private val grouchoWalk = Exercise(
    "Groucho Walk", "Legs",
    "Drop into a deep squat with your back straight and walk forward while staying low, like Groucho Marx."
)
private val calfRaises = Exercise(
    "Calf Raises", "Legs",
    "Rise onto your toes and lower under control. Spend a third of the time in each foot position. Start with your toes pointed in so your feet make an upside-down V, then switch to feet straight, then finish with your toes pointed out so your feet make a V.",
    cues = listOf((2.0 / 3.0) to "Switch to feet straight.", (1.0 / 3.0) to "Switch to toes out.")
)
private val siebersSquats = Exercise(
    "80/20 Siebers-Speed Squats", "Legs",
    "A fast squat with most of your weight (about 80 percent) on one leg and a little (20 percent) on the other. Switch legs at the halfway call.",
    switchSides = true,
    videoQuery = "80/20 Siebers speed squats p90x"
)

private val legsBackSteps =
    work(
        balancedLunge, calfRaiseSquat, proneW, superSkaters, wallSquat, proneY,
        stepBackLunge, alternatingSideLunge, proneT, singleLegWallSquat, deadliftSquat, proneYWSwitch
    ) + rest("Water break", 120) +
    work(
        threeWayLunge, sneakyLunge, proneW, chairSalutations, toeRollIsoLunge, proneY,
        grouchoWalk, calfRaises, proneT, siebersSquats, proneYWSwitch
    ) + rest("Cool down", 258)

// ---------------------------------------------------------------------------------------------
// Abs (quantity based: 25 reps each, tap Done).
// ---------------------------------------------------------------------------------------------

private const val ABS_TARGET = 25

private val inAndOuts = Exercise(
    "In & Outs", "Abs",
    "Sit with your hands behind you, lean back and lift your feet. Pull your knees in toward your chest, then extend your legs out.",
    reps = ABS_TARGET
)
private val seatedBicycles = Exercise(
    "Seated Bicycles", "Abs",
    "Sit leaning back with your hands behind you and your feet off the floor. Pedal your legs like a bicycle.",
    reps = ABS_TARGET
)
private val reverseBicycles = Exercise(
    "Reverse Bicycles", "Abs",
    "Lie on your back with your hands under your hips. Lift your legs and pedal backward, bringing your knees toward your chest.",
    reps = ABS_TARGET
)
private val crunchyFrogs = Exercise(
    "Seated Crunchy Frogs", "Abs",
    "Sit leaning back with your feet together and your knees open like a frog. Pull your knees and chest toward each other, then open back out.",
    reps = ABS_TARGET,
    videoQuery = "seated crunchy frogs exercise"
)
private val crossedWideSitUps = Exercise(
    "Crossed Leg/Wide Leg Sit-Ups", "Abs",
    "Do sit-ups, alternating each rep between legs crossed and legs spread wide.",
    reps = ABS_TARGET
)
private val fiferScissors = Exercise(
    "Fifer Scissors", "Abs",
    "Lie on your back with your hands under your hips and your legs straight and low. Scissor your legs up and down in small alternating kicks, keeping your lower back pressed down.",
    reps = ABS_TARGET,
    videoQuery = "fifer scissors exercise"
)
private val hipRockRaise = Exercise(
    "Hip Rock 'N' Raise", "Abs",
    "Lie on your back with your legs straight up. Rock your hips up off the floor toward the ceiling, then lower with control.",
    reps = ABS_TARGET,
    videoQuery = "hip rock and raise exercise"
)
private val pulseUps = Exercise(
    "Pulse-Ups (Heels to Heaven)", "Abs",
    "Lie on your back with your legs straight up. Pulse your heels toward the ceiling by lifting your hips a few inches.",
    reps = ABS_TARGET
)
private val rollUpVUpCombo = Exercise(
    "Roll-Up/V-Up Combos", "Abs",
    "Lie flat with your arms overhead. Roll up to sitting and reach for your toes, then lie back and lift your legs and arms into a V. Alternate roll-ups and V-ups.",
    reps = ABS_TARGET
)
private val obliqueVUps = Exercise(
    "Oblique V-Ups", "Abs",
    "Lie on your side. Lift your legs and upper body toward each other in a side V. Switch sides about halfway through.",
    reps = ABS_TARGET
)
private val legClimbs = Exercise(
    "Leg Climbs", "Abs",
    "Lie on your back with your legs straight up. Reach one hand, then the other, up your legs toward your toes like climbing a rope.",
    reps = ABS_TARGET
)
private val masonTwist = Exercise(
    "Mason (Kayak) Twist", "Abs",
    "Sit leaning back with your feet off the floor. Twist side to side, touching your hands to the floor beside your hips each time.",
    reps = ABS_TARGET,
    videoQuery = "mason twist exercise"
)

private val absSteps = work(
    inAndOuts, seatedBicycles, reverseBicycles, crunchyFrogs, crossedWideSitUps, fiferScissors,
    hipRockRaise, pulseUps, rollUpVUpCombo, obliqueVUps, legClimbs, masonTwist
)

// ---------------------------------------------------------------------------------------------
// Warm-up (about 8 minutes, no equipment). Shorter moves than the workouts.
// ---------------------------------------------------------------------------------------------

private const val WARM_UP_SECONDS = 40

private val warmJacks = Exercise(
    "Easy Jumping Jacks", "Warm-up",
    "Light jumping jacks at an easy pace to raise your heart rate. Stay relaxed and breathe steadily.",
    seconds = WARM_UP_SECONDS
)
private val warmArmCircles = Exercise(
    "Arm Circles", "Warm-up",
    "Arms straight out to the sides. Make big, slow circles forward, then reverse at the halfway call.",
    seconds = WARM_UP_SECONDS,
    halfwayCue = "Reverse direction."
)
private val warmTorsoTwists = Exercise(
    "Torso Twists", "Warm-up",
    "Feet shoulder-width apart. Rotate your torso side to side, letting your arms swing loosely.",
    seconds = WARM_UP_SECONDS
)
private val warmHipCircles = Exercise(
    "Hip Circles", "Warm-up",
    "Hands on your hips. Draw big, slow circles with your hips, then reverse at the halfway call.",
    seconds = WARM_UP_SECONDS,
    halfwayCue = "Reverse direction."
)
private val warmLegSwings = Exercise(
    "Leg Swings", "Warm-up",
    "Hold a wall or chair for balance. Swing one leg forward and back in a smooth arc, keeping your torso upright. Switch legs at the halfway call.",
    seconds = WARM_UP_SECONDS,
    switchSides = true
)
private val warmSquats = Exercise(
    "Slow Bodyweight Squats", "Warm-up",
    "Feet shoulder-width apart. Sit your hips back and down with your chest up, then stand tall. Go slowly and smoothly.",
    seconds = WARM_UP_SECONDS
)
private val warmLunges = Exercise(
    "Reverse Lunges with Reach", "Warm-up",
    "Step one foot back into a lunge and reach both arms overhead. Step back to standing and alternate legs.",
    seconds = WARM_UP_SECONDS
)
private val warmCatCow = Exercise(
    "Cat-Cow", "Warm-up",
    "On your hands and knees. Arch your back and look up, then round your back and tuck your chin. Move with your breath.",
    seconds = WARM_UP_SECONDS,
    videoQuery = "cat cow stretch exercise"
)
private val warmInchworms = Exercise(
    "Inchworms", "Warm-up",
    "Stand tall, fold forward and walk your hands out to a plank, then walk them back to your feet and stand up. Keep your legs as straight as is comfortable.",
    seconds = WARM_UP_SECONDS
)
private val warmScapularPushUps = Exercise(
    "Scapular Push-Ups", "Warm-up",
    "In a plank with straight arms, let your chest sink between your shoulder blades, then push the floor away to spread them apart. Only your shoulder blades move.",
    seconds = WARM_UP_SECONDS,
    videoQuery = "scapular push ups exercise"
)

private val warmUpSteps = work(
    warmJacks, warmArmCircles, warmTorsoTwists, warmHipCircles, warmLegSwings,
    warmSquats, warmLunges, warmCatCow, warmInchworms, warmScapularPushUps
)

// ---------------------------------------------------------------------------------------------
// Chest, shoulders and triceps (dumbbells, a sturdy chair).
// ---------------------------------------------------------------------------------------------

private val slowMoPushUp = Exercise(
    "Slow-Motion 3-in-1 Push-Ups", "Chest",
    "Slow push-ups, about 3 seconds down and 3 seconds up. Do one rep with your hands wide, one with your hands shoulder-width and one with your hands close together, then repeat.",
    videoQuery = "slow motion 3 in 1 push ups p90x"
)
private val inOutShoulderFly = Exercise(
    "In & Out Shoulder Flys", "Shoulders",
    "Dumbbells at your sides. Raise them out to the sides to shoulder height, lower, then raise them in front of you with your hands close together. Alternate out and in.",
    weighted = true,
    videoQuery = "in and out shoulder flys p90x"
)
private val plangePushUp = Exercise(
    "Plange Push-Ups", "Chest",
    "Place your hands back near your hips and lean forward so your shoulders move ahead of your hands. Lower your chest toward the floor and press back up. Drop to your knees if you need to.",
    videoQuery = "planche push ups p90x"
)
private val pikePress = Exercise(
    "Pike Presses", "Shoulders",
    "Hands and feet on the floor with your hips high, making an upside-down V. Bend your elbows to lower the top of your head toward the floor, then press back up."
)
private val floorFly = Exercise(
    "Floor Flys", "Chest",
    "Lie on your back holding the dumbbells above your chest. With a slight bend in your elbows, open your arms until your upper arms touch the floor, then squeeze them back together.",
    weighted = true
)
private val scarecrow = Exercise(
    "Scarecrows", "Shoulders",
    "Raise your arms out to the sides to shoulder height with your elbows bent 90 degrees and your forearms pointing up. Rotate your forearms down until they point toward the floor, then back up.",
    weighted = true,
    videoQuery = "scarecrow shoulder exercise dumbbell"
)
private val overheadTricepsExtension = Exercise(
    "Overhead Tricep Extensions", "Triceps",
    "Hold one dumbbell with both hands behind your head. Straighten your arms to press it overhead, then lower it slowly behind your head.",
    weighted = true
)
private val twoTwitchPushUp = Exercise(
    "Two-Twitch Speed Push-Ups", "Chest",
    "Fast push-ups. Lower halfway and press back up twice in quick pulses, then lower all the way and press up. That is one rep.",
    videoQuery = "two twitch speed push ups p90x"
)
private val yPress = Exercise(
    "Y-Presses", "Shoulders",
    "Dumbbells at your shoulders. Press them up and out so your arms finish in a Y shape overhead, then lower back to your shoulders.",
    weighted = true
)
private val sideToSidePushUp = Exercise(
    "Side-to-Side Push-Ups", "Chest",
    "Lower your chest toward one hand while the other arm stays straight, press up, then shift across and lower toward the other hand."
)
private val pourFly = Exercise(
    "Pour Flys", "Chest",
    "Raise your arms out to the sides to shoulder height, then rotate your wrists as if pouring a pitcher, pinkies up. Rotate back and lower.",
    weighted = true,
    videoQuery = "pour flys p90x"
)
private val sideLeaningTricepsExtension = Exercise(
    "Side-Leaning Tricep Extensions", "Triceps",
    "Lean to one side holding a dumbbell bent behind your head with one arm. Straighten the arm overhead, then lower it. Switch arms at the halfway call.",
    switchSides = true,
    weighted = true,
    videoQuery = "side leaning tricep extension p90x"
)
private val oneArmPushUp = Exercise(
    "One-Arm Push-Ups", "Chest",
    "Feet wide for balance, one hand on the floor under your chest and the other behind your back. Lower and press with the one arm. Use your knees if you need to. Switch arms at the halfway call.",
    switchSides = true
)
private val weightedCircles = Exercise(
    "Weighted Circles", "Shoulders",
    "Hold the dumbbells with your arms straight out to the sides at shoulder height. Make small, controlled circles. Reverse the direction at the halfway call.",
    halfwayCue = "Reverse direction.",
    weighted = true
)
private val throwTheBomb = Exercise(
    "Throw the Bomb", "Shoulders",
    "Hold one dumbbell back by your shoulder. Push it forward and up like a throw, then pull it back. Switch arms at the halfway call.",
    switchSides = true,
    weighted = true,
    videoQuery = "throw the bomb p90x"
)
private val plyoPushUp = Exercise(
    "Clap or Plyo Push-Ups", "Chest",
    "Lower your chest, then push up explosively so your hands leave the floor. Clap if you can, or just push off the floor. Drop to your knees if needed."
)
private val sloMoThrow = Exercise(
    "Slo-Mo Throws", "Shoulders",
    "The same motion as Throw the Bomb, but slow. Push the dumbbell forward and up for 3 seconds and return for 3 seconds. Switch arms at the halfway call.",
    switchSides = true,
    weighted = true,
    videoQuery = "slo mo throws p90x"
)
private val frontToBackTricepsExtension = Exercise(
    "Front-to-Back Tricep Extensions", "Triceps",
    "Hinge forward at the hips with your elbows bent in front of you. Extend both arms in a long arc to straight behind you, squeeze, then bring them back to the front.",
    weighted = true
)
private val oneArmBalancePushUp = Exercise(
    "One-Arm Balance Push-Ups", "Chest",
    "Put one hand on a dumbbell, rolled towel or book and the other on the floor. Do push-ups in that uneven position. Switch hands at the halfway call.",
    switchSides = true
)
private val flyRowPress = Exercise(
    "Fly-Row Presses", "Shoulders",
    "Bent forward, do a rear fly, then a row, then stand and press the dumbbells overhead. That is one rep.",
    weighted = true
)
private val crossBodyBlows = Exercise(
    "Dumbbell Cross-Body Blows", "Shoulders",
    "Stand with a dumbbell in each hand at your chest. Punch one across your body, pull it back, then punch with the other. Keep your core tight.",
    weighted = true
)

private val chestShouldersTricepsSteps =
    work(
        slowMoPushUp, inOutShoulderFly, chairDips, plangePushUp,
        pikePress, sideTriRise, floorFly, scarecrow
    ) + rest("Water break", 45) +
    work(
        overheadTricepsExtension, twoTwitchPushUp, yPress, lyingTricepsExtension,
        sideToSidePushUp, pourFly, sideLeaningTricepsExtension, oneArmPushUp
    ) + rest("Water break", 45) +
    work(
        weightedCircles, throwTheBomb, plyoPushUp, sloMoThrow,
        frontToBackTricepsExtension, oneArmBalancePushUp, flyRowPress, crossBodyBlows
    ) + rest("Cool down", 180)

// ---------------------------------------------------------------------------------------------
// Back and biceps (dumbbells, a sturdy chair). Pull-ups and chin-ups are replaced with rows.
// ---------------------------------------------------------------------------------------------

private val wideRow = Exercise(
    "Wide Bent-Over Rows", "Back",
    "Replaces wide front pull-ups. Hinge forward with a flat back and your palms facing back. Pull the dumbbells toward your lower chest with your elbows flared wide, squeeze your shoulder blades, then lower.",
    weighted = true
)
private val lawnmower = Exercise(
    "Lawnmowers", "Back",
    "Hinge forward with one hand braced on a chair or your knee. Row the other dumbbell up and back as if starting a lawnmower, then lower. Switch arms at the halfway call.",
    switchSides = true,
    weighted = true,
    videoQuery = "lawnmower exercise dumbbell back"
)
private val twentyOnes = Exercise(
    "Twenty-Ones", "Biceps",
    "Seven curls from the bottom to halfway, then seven from halfway to the top, then seven full curls. That is one set of twenty-one. Repeat.",
    weighted = true
)
private val crossBodyCurl = Exercise(
    "One-Arm Cross-Body Curls", "Biceps",
    "Curl one dumbbell up and across your body toward the opposite shoulder with your palm facing in, then lower. Switch arms at the halfway call.",
    switchSides = true,
    weighted = true
)
private val switchGripRow = Exercise(
    "Switch-Grip Bent-Over Rows", "Back",
    "Replaces switch-grip pull-ups. Hinge forward and row with your palms facing back, then flip to palms facing forward for the next rep. Alternate every rep.",
    weighted = true
)
private val elbowsOutLawnmower = Exercise(
    "Elbows-Out Lawnmowers", "Back",
    "A lawnmower row, but drive your elbow out and up to the side to hit the upper back. Switch arms at the halfway call.",
    switchSides = true,
    weighted = true,
    videoQuery = "elbows out lawnmowers p90x"
)
private val standingCurl = Exercise(
    "Standing Bicep Curls", "Biceps",
    "Stand tall with the dumbbells at your sides, palms forward. Curl both up with your elbows pinned, squeeze, then lower slowly.",
    weighted = true
)
private val oneArmConcentrationCurl = Exercise(
    "One-Arm Concentration Curls", "Biceps",
    "Sit with your elbow braced against your inner thigh. Curl the dumbbell up, squeeze, then lower slowly. Switch arms at the halfway call.",
    switchSides = true,
    weighted = true
)
private val oneArmRow = Exercise(
    "One-Arm Rows", "Back",
    "Replaces corn cob pull-ups. Brace one hand on a chair and row the other dumbbell up to your hip, squeeze your back, then lower. Switch arms at the halfway call.",
    switchSides = true,
    weighted = true
)
private val reverseGripRow = Exercise(
    "Reverse Grip Bent-Over Rows", "Back",
    "Hinge forward with a flat back and your palms facing forward. Pull the dumbbells to your waist with your elbows close to your sides, then lower.",
    weighted = true
)
private val openArmCurl = Exercise(
    "Open Arm Curls", "Biceps",
    "Hold the dumbbells out to your sides at shoulder height with your palms up. Curl them toward your shoulders, then open your arms back out.",
    weighted = true,
    videoQuery = "open arm curls p90x"
)
private val dumbbellPullover = Exercise(
    "Dumbbell Pullovers", "Back",
    "Replaces towel pull-ups. Lie on your back holding one dumbbell with both hands over your chest. Keep your arms nearly straight and lower the weight back over your head, then pull it back over your chest.",
    weighted = true
)
private val congdonLocomotive = Exercise(
    "Congdon Locomotives", "Biceps",
    "Hold the dumbbells with your elbows bent at 90 degrees. Alternate arms in a quick piston motion, one curling up as the other lowers, like train wheels.",
    weighted = true,
    videoQuery = "congdon locomotives p90x"
)
private val oneArmCorkscrewCurl = Exercise(
    "One-Arm Corkscrew Curls", "Biceps",
    "Curl one dumbbell up while twisting your palm from facing in to facing up, then lower and untwist. Switch arms at the halfway call.",
    switchSides = true,
    weighted = true
)
private val closeGripUnderhandRow = Exercise(
    "Close-Grip Underhand Rows", "Back",
    "Replaces chin-ups. Hinge forward with your palms facing forward and the dumbbells close together. Pull them to your lower ribs with your elbows tight to your body, then lower.",
    weighted = true
)
private val seatedBackFly = Exercise(
    "Seated Bent-Over Back Flys", "Back",
    "Sit on the edge of a chair and lean forward with the dumbbells hanging. Raise your arms out to the sides, squeezing your shoulder blades, then lower.",
    weighted = true
)
private val curlUpHammerDown = Exercise(
    "Curl-Up/Hammer Downs", "Biceps",
    "Curl the dumbbells up with your palms facing up. Turn your palms to face each other at the top and lower slowly with a hammer grip.",
    weighted = true
)
private val hammerCurl = Exercise(
    "Hammer Curls", "Biceps",
    "Palms facing each other, curl both dumbbells up with your elbows pinned, then lower slowly.",
    weighted = true
)
private val maxRepRow = Exercise(
    "Max Rep Dumbbell Rows", "Back",
    "Replaces max-rep pull-ups. Hinge forward and row the dumbbells to your lower chest for as many clean reps as you can.",
    weighted = true
)
private val inOutHammerCurl = Exercise(
    "In-Out Hammer Curls", "Biceps",
    "Hammer curls that alternate between hands close to your body (in) and hands wide (out) each rep.",
    weighted = true
)
private val stripSetCurl = Exercise(
    "Strip-Set Curls", "Biceps",
    "Curl with a heavier weight until the halfway call, then switch straight to lighter dumbbells and keep curling.",
    halfwayCue = "Switch to lighter weights.",
    weighted = true
)

private val backBicepsSteps =
    work(
        wideRow, lawnmower, twentyOnes, crossBodyCurl,
        switchGripRow, elbowsOutLawnmower, standingCurl, oneArmConcentrationCurl
    ) + rest("Water break", 45) +
    work(
        oneArmRow, reverseGripRow, openArmCurl, staticArmCurl,
        dumbbellPullover, congdonLocomotive, cohenCurl, oneArmCorkscrewCurl
    ) + rest("Water break", 45) +
    work(
        closeGripUnderhandRow, seatedBackFly, curlUpHammerDown, hammerCurl,
        maxRepRow, supermanLifts, inOutHammerCurl, stripSetCurl
    ) + rest("Cool down", 180)

// ---------------------------------------------------------------------------------------------
// Cardio circuit (about 42 minutes, no equipment). Every move is 30 seconds with a short get-ready.
// ---------------------------------------------------------------------------------------------

private fun cardio(name: String, how: String, query: String = "$name insanity plyometric cardio circuit") =
    Exercise(name, "Cardio", how, seconds = 30, introSeconds = 3, videoQuery = query)

private val jogInPlace = cardio("Jog in Place", "Jog on the spot. Pick up the pace a little each round.")
private val jumpingJacks = cardio("Jumping Jacks", "Jump your feet out and your arms overhead, then back together.")
private val heisman = cardio(
    "Heisman",
    "Stay low and run in place with quick high steps, swinging your feet out to the sides like a football player dodging a tackle."
)
private val heisman123 = cardio("1-2-3 Heisman", "Heisman steps in a repeating count: step, step, step, then change direction.")
private val buttKicks = cardio("Butt Kicks", "Jog in place, kicking your heels up toward your backside.")
private val highKnees = cardio("High Knees", "Run in place, driving your knees up to hip height.")
private val mummyKicks = cardio(
    "Mummy Kicks",
    "Hold your arms straight out in front like a mummy. Kick each straight leg up to touch your hand, alternating."
)
private val suicideDrills = cardio("Suicide Drills", "Run from side to side, touching the floor each time you reach the end of your line.")
private val powerSquats = cardio("Power Squats", "Squat low, then jump up as high as you can and land softly into the next squat.")
private val mountainClimbers = cardio("Mountain Climbers", "In a plank, drive your knees toward your chest one after the other, as fast as you can keep good form.")
private val skiJumps = cardio("Ski Jumps", "Keep your legs together and hop from side to side like a skier, landing softly.")
private val switchFeet = cardio("Switch Feet", "Start in a lunge, jump and switch your legs in the air, landing in a lunge on the other side (scissor jumps).")
private val footballSprints = cardio("Football Wide Sprints", "Sprint in place in a wide stance, then drop to the floor and get back up sprinting.")
private val basketballDrills = cardio("Basketball Drills", "Bend down and pick up an imaginary ball, then jump up and shoot it. Repeat.")
private val levelOneDrills = cardio(
    "Level 1 Drills",
    "Plank position, 4 push-ups, then 8 fast sprints with your hands on the floor, then jump up. Repeat."
)
private val skiAbs = cardio("Ski Abs", "In a plank, jump both feet toward one shoulder, back to plank, then toward the other shoulder.")
private val inOutAbs = cardio("In/Out Abs", "In a plank, jump your feet in between your hands, then jump them back out.")
private val jabs = cardio("Jabs", "Fight stance, throwing quick straight punches with the lead hand.", "jabs kickboxing cardio workout")
private val crossJacks = cardio("Cross Jacks", "Jumping jacks, crossing your arms in front of your chest each time you jump.")
private val upperCuts = cardio("Upper Cuts", "Fight stance, punching upward from your hips with alternating hands.", "upper cuts kickboxing cardio workout")
private val attackPunches = cardio("Attack Punches", "Throw fast, hard punches forward with alternating hands as you move.", "attack punches kickboxing cardio workout")

private val cardioCircuitSteps: List<Step> = buildList {
    val warm = listOf(jogInPlace, jumpingJacks, heisman, heisman123, buttKicks, highKnees, mummyKicks)
    for (round in 1..3) {
        if (round > 1) add(Step.Rest("Round $round of 3. Go a little faster.", 5))
        warm.forEach { add(Step.Work(it)) }
    }
    add(Step.Rest("Water break", 30))
    add(Step.Rest("Stretch", 420))

    val circuitOne = listOf(suicideDrills, powerSquats, mountainClimbers, skiJumps)
    for (round in 1..3) {
        circuitOne.forEach { add(Step.Work(it)) }
        if (round < 3) add(Step.Rest("Water break", 30))
    }
    add(Step.Work(switchFeet))
    add(Step.Work(footballSprints))
    add(Step.Rest("Water break", 30))

    val circuitTwo = listOf(basketballDrills, levelOneDrills, skiAbs, inOutAbs)
    for (round in 1..3) {
        circuitTwo.forEach { add(Step.Work(it)) }
        if (round < 3) add(Step.Rest("Water break", 30))
    }
    listOf(jabs, crossJacks, upperCuts, attackPunches).forEach { add(Step.Work(it)) }
    add(Step.Rest("Cool down: stretch and breathe", 210))
}

object Days {
    /** Every built-in exercise once, for building custom workouts. */
    val exercisePool: List<Exercise> by lazy {
        all.flatMap { day -> day.steps.filterIsInstance<Step.Work>().map { it.exercise } }
            .distinctBy { it.name }
    }

    fun fromCustom(workout: CustomWorkout): Day {
        val steps: List<Step> = workout.exercises
            .mapNotNull { name -> exercisePool.firstOrNull { it.name == name } }
            .map { Step.Work(it) }
        return Day(
            id = "custom-${workout.id}",
            title = workout.title,
            blurb = "Your own workout.",
            steps = steps,
            met = 4.5
        )
    }

    fun isCustom(id: String): Boolean = id.startsWith("custom-")

    fun find(id: String?): Day? =
        all.firstOrNull { it.id == id }
            ?: Storage.custom.value.firstOrNull { "custom-${it.id}" == id }?.let { fromCustom(it) }

    val all: List<Day> = listOf(
        Day("warm-up", "Warm-Up", "Easy moves to loosen up before any workout. No equipment.", warmUpSteps, met = 3.0, trackReps = false),
        Day("chest-back", "Chest & Back", "Push-ups plus bodyweight back work. No equipment, a chair or couch for decline push-ups.", chestBackSteps, met = 5.0),
        Day("arms", "Shoulders & Arms", "Shoulders, biceps and triceps. Dumbbells and a sturdy chair.", armsSteps, met = 4.5),
        Day("legs-back", "Legs & Back", "Lunges, squats and bodyweight back work. No equipment.", legsBackSteps, met = 5.0),
        Day("abs", "Abs", "12 moves, 25 reps each. Tap Done after each one.", absSteps, met = 3.8),
        Day("chest-shoulders-triceps", "Chest, Shoulders & Triceps", "Push-ups, flys and presses. Dumbbells and a sturdy chair.", chestShouldersTricepsSteps, met = 5.0),
        Day("back-biceps", "Back & Biceps", "Rows and curls. Dumbbells and a sturdy chair. Pull-ups are replaced with rows.", backBicepsSteps, met = 4.5),
        Day(
            "cardio-circuit", "Cardio Circuit",
            "About 42 minutes of jumping and sprinting, no equipment. Warm-up, two circuits of three rounds, kickboxing finishers and a cool down.",
            cardioCircuitSteps, met = 8.0, trackReps = false
        )
    )
}
