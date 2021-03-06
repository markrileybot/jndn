/**
 * Copyright (C) 2015-2018 Regents of the University of California.
 * @author: Jeff Thompson <jefft0@remap.ucla.edu>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * A copy of the GNU Lesser General Public License is in the file COPYING.
 */

package net.named_data.jndn.tests;

import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.ProtobufTlv;
import net.named_data.jndn.tests.ChannelStatusProto.ChannelStatusMessage;
import net.named_data.jndn.util.Blob;
import net.named_data.jndn.util.SegmentFetcher;
import net.named_data.jndn.security.KeyChain;

/**
 * This sends a faces channels request to the local NFD and prints the response.
 * This is equivalent to the NFD command line command "nfd-status -c".
 * See http://redmine.named-data.net/projects/nfd/wiki/Management .
 */
public class TestListChannels {
  public static void
  main(String[] args)
  {
    try {
      // The default Face connects to the local NFD.
      Face face = new Face();

      Interest interest = new Interest(new Name("/localhost/nfd/faces/channels"));
      interest.setInterestLifetimeMilliseconds(4000);
      System.out.println("Express interest " + interest.getName().toUri());

      final boolean[] enabled = new boolean[] { true };
      SegmentFetcher.fetch
        (face, interest, (KeyChain)null,
         new SegmentFetcher.OnComplete() {
           public void onComplete(Blob content) {
             enabled[0] = false;
             printChannelStatuses(content);
           }},
         new SegmentFetcher.OnError() {
           public void onError(SegmentFetcher.ErrorCode errorCode, String message) {
             enabled[0] = false;
             System.out.println(message);
           }});

      // Loop calling processEvents until a callback sets enabled[0] = false.
      while (enabled[0]) {
        face.processEvents();

        // We need to sleep for a few milliseconds so we don't use 100% of
        //   the CPU.
        Thread.sleep(5);
      }
    }
    catch (Exception e) {
       System.out.println("exception: " + e.getMessage());
    }
  }

  /**
   * This is called when all the segments are received to decode the
   * encodedMessage repeated TLV ChannelStatus messages and display the values.
   * @param encodedMessage The repeated TLV-encoded ChannelStatus.
   */
  public static void
  printChannelStatuses(Blob encodedMessage)
  {
    ChannelStatusMessage.Builder channelStatusMessage = ChannelStatusMessage.newBuilder();
    try {
      ProtobufTlv.decode(channelStatusMessage, encodedMessage);
    } catch (EncodingException ex) {
      System.out.println("Error decoding the ChannelStatus message: " + ex.getMessage());
    }

    System.out.println("Channels:");
    for (int iEntry = 0; iEntry < channelStatusMessage.getChannelStatusCount(); ++iEntry) {
      ChannelStatusMessage.ChannelStatus channelStatus = channelStatusMessage.getChannelStatus(iEntry);

      // Format to look the same as "nfd-status -c".
      System.out.println("  " + channelStatus.getLocalUri());
    }
  }
}
