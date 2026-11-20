/**
 * Memberships: the gym the user pays for, the visits made and what each visit has really cost.
 * Listens to {@link dev.gymscout.training.SessionLogged} so a workout at the member gym counts as a
 * visit without logging it twice.
 */
@ApplicationModule(displayName = "Memberships")
package dev.gymscout.memberships;

import org.springframework.modulith.ApplicationModule;
