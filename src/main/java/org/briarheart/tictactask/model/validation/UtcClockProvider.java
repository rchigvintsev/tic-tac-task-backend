package org.briarheart.tictactask.model.validation;

import javax.validation.ClockProvider;
import java.time.Clock;

/**
 * Implementation of {@link ClockProvider} which returns the current system time in UTC time zone using
 * {@link Clock#systemUTC()}.
 *
 * @author Roman Chigvintsev
 */
public class UtcClockProvider implements ClockProvider {
    /**
     * Returns the current system time in UTC time zone.
     *
     * @return instance of {@link Clock} in UTC time zone
     */
    @Override
    public Clock getClock() {
        return Clock.systemUTC();
    }
}
