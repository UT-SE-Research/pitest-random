package org.pitest.mutationtest.execute;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.pitest.mutationtest.MutationStatusMap;
import org.pitest.mutationtest.MutationStatusTestPair;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.process.ProcessArgs;
import org.pitest.process.WrappingProcess;
import org.pitest.util.CommunicationThread;
import org.pitest.util.ExitCode;
import org.pitest.util.Log;
import org.pitest.util.Verbosity;

public class MutationTestProcess {

  private static final Logger LOG = Log.getLogger();

  private final WrappingProcess process;
  private final CommunicationThread thread;
  private final Map<MutationIdentifier, MutationStatusTestPair> idMap;

  public MutationTestProcess(final ServerSocket socket,
      final ProcessArgs processArgs, final MinionArguments arguments) {
    this.process = new WrappingProcess(socket.getLocalPort(), processArgs,
        MutationTestMinion.class);

    this.idMap = new ConcurrentHashMap<>();
    this.thread = new CommunicationThread(socket, new SendData(arguments), new Receive(idMap));

  }

  public void start() throws IOException, InterruptedException {
    this.thread.start();
    this.process.start();
  }

  public void results(final MutationStatusMap allmutations) throws IOException {

    for (final MutationDetails each : allmutations.allMutations()) {
      final MutationStatusTestPair status = this.idMap.get(each.getId());
      if (status != null) {
        allmutations.setStatusForMutation(each, status);
      }
    }

  }

  public ExitCode waitToDie() {
    try {
      // Wait a moment to give the monitoring thread time to finish naturally. This
      // happens when the monitored process sends a "DONE" signal over the socket,
      // the process itself should exit shortly after sending the signal.
      // Most likely the process will still be running
      Optional<ExitCode> maybeExit = this.thread.waitToFinish(5);

      // While the monitored process reports being alive, keep polling
      // the monitoring thread to see if it has finished.
      while (!maybeExit.isPresent() && this.process.isAlive()) {
        maybeExit = this.thread.waitToFinish(10);
      }

      // Either the monitored process died, or the thread ended.
      // Check the thread one last time to try and avoid reporting
      // an error code if it was the process that went down first
      maybeExit = this.thread.waitToFinish(10);

      // If the monitored thread is still live, but the process is dead
      // then either the process never properly started or it died
      // before reporting its exit
      return maybeExit.orElse(ExitCode.MINION_DIED);
    } finally {
        boolean exitedAfterDestroy = false;
        boolean exitedAfterForce = false;

        // 1) Try graceful shutdown first
        try {
            this.process.destroy();
            exitedAfterDestroy = this.process.waitFor(200);
        } catch (final InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (final Throwable ignore) {
            // ignore
        }

        // 2) If still alive, force kill
        if (this.process.isAlive()) {
            if (Log.verbosity() == Verbosity.RANDOM_VERBOSE) {
                LOG.info("RANDOM LOG: forcing minion kill at " + System.nanoTime() + " ns.");
            }
            try {
                this.process.destroyForcibly();
                exitedAfterForce = this.process.waitFor(5000);
            } catch (final InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (final Throwable ignore) {
                // ignore
            }
        }

        // 3) Final state log
        if (Log.verbosity() == Verbosity.RANDOM_VERBOSE) {
            LOG.info("RANDOM LOG: cleanup_done exitedAfterDestroy=" + exitedAfterDestroy
                    + " exitedAfterForce=" + exitedAfterForce
                    + " alive_after_cleanup=" + this.process.isAlive()
                    + " at " + System.nanoTime() + " ns.");
        }
    }

  }

}
