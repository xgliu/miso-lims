/*
 * Copyright (c) 2012. The Genome Analysis Centre, Norwich, UK
 * MISO project contacts: Robert Davey, Mario Caccamo @ TGAC
 * *********************************************************************
 *
 * This file is part of MISO.
 *
 * MISO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MISO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MISO.  If not, see <http://www.gnu.org/licenses/>.
 *
 * *********************************************************************
 */

package uk.ac.bbsrc.tgac.miso.notification.service;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.integration.Message;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import uk.ac.bbsrc.tgac.miso.core.util.LimsUtils;
import uk.ac.bbsrc.tgac.miso.core.util.SubmissionUtils;
import uk.ac.bbsrc.tgac.miso.notification.util.NotificationUtils;
import uk.ac.bbsrc.tgac.miso.tools.run.util.FileSetTransformer;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * uk.ac.bbsrc.tgac.miso.notification.util
 * <p/>
 * Transforms relevant Illumina metadata files into a Map to form the payload of a Message
 *
 * @author Rob Davey
 * @date 10-Dec-2010
 * @since 0.1.6
 */
public class IlluminaTransformer implements FileSetTransformer<String, String, File> {
  protected static final Logger log = LoggerFactory.getLogger(IlluminaTransformer.class);

  private final Pattern runCompleteLogPattern = Pattern.compile(
          "(\\d{1,2}\\/\\d{1,2}\\/\\d{4},\\d{2}:\\d{2}:\\d{2})\\.\\d{3},\\d+,\\d+,\\d+,Proce[s||e]sing\\s+completed\\.\\s+Run\\s+has\\s+finished\\."
  );

  private final Pattern lastDateEntryLogPattern = Pattern.compile(
          "(\\d{1,2}\\/\\d{1,2}\\/\\d{4},\\d{2}:\\d{2}:\\d{2})\\.\\d{3},\\d+,\\d+,\\d+,.*"
  );

  private final DateFormat logDateFormat = new SimpleDateFormat("MM'/'dd'/'yyyy','HH:mm:ss");

  public Map<String, String> transform(Message<Set<File>> message) {
    return transform(message.getPayload());
  }

  public Map<String, String> transform(Set<File> files) {
    log.info("Processing " + files.size() + " Illumina run directories...");

    int count = 0;

    //TODO modify this to use a JSONObject instead of a Map
    HashMap<String, JSONArray> map = new HashMap<String, JSONArray>();

    map.put("Running", new JSONArray());
    map.put("Completed", new JSONArray());
    map.put("Unknown", new JSONArray());

    for (File rootFile : files) {
      count++;
      String countStr = "[#"+count+"/"+files.size()+"] ";
      if (rootFile.isDirectory()) {
        if (rootFile.canRead()) {
          JSONObject run = new JSONObject();
          File oldStatusFile = new File(rootFile, "/Data/Status.xml");
          File newStatusFile = new File(rootFile, "/Data/reports/Status.xml");
          File runParameters = new File(rootFile, "/runParameters.xml");
          File runInfo = new File(rootFile, "/RunInfo.xml");
          File completeFile = new File(rootFile, "/Run.completed");

          try {
            boolean complete = true;
            String runName = rootFile.getName();

            run.put("runName", runName);
            run.put("fullPath", rootFile.getCanonicalPath()); //follow symlinks!

            if (!oldStatusFile.exists() && !newStatusFile.exists()) {
              //probably MiSeq
              File otherRunParameters = new File(rootFile, "/RunParameters.xml");
              File lastCycleLogFile = null;
              if (runInfo.exists()) {
                run.put("runinfo", SubmissionUtils.transform(runInfo));

                Document runInfoDoc = SubmissionUtils.emptyDocument();
                SubmissionUtils.transform(runInfo, runInfoDoc);

                //runName = statusDoc.getElementsByTagName("RunName").item(0).getTextContent();
                //run.put("runName", runName);
                //run.put("sequencerName", statusDoc.getElementsByTagName("InstrumentName").item(0).getTextContent());

                int numReads = runInfoDoc.getElementsByTagName("Read").getLength();
                int numCycles = 0;
                NodeList nl = runInfoDoc.getElementsByTagName("Read");
                for (int i = 0; i < nl.getLength(); i++) {
                  Element e = (Element)nl.item(i);
                  if (!"".equals(e.getAttributeNS(null, "NumCycles"))) {
                    numCycles += Integer.parseInt(e.getAttributeNS(null, "NumCycles"));
                  }
                }

                lastCycleLogFile = new File(rootFile, "/Logs/"+runName+"_Cycle"+numCycles+"_Log.00.log");

                //int imgCycle = new Integer(statusDoc.getElementsByTagName("ImgCycle").item(0).getTextContent());
                //int scoreCycle = new Integer(statusDoc.getElementsByTagName("ScoreCycle").item(0).getTextContent());
                //int callCycle = new Integer(statusDoc.getElementsByTagName("CallCycle").item(0).getTextContent());

                run.put("numCycles", numCycles);

                if (!new File(rootFile, "/Basecalling_Netcopy_complete_SINGLEREAD.txt").exists()) {
                  for (int i = 0; i < numReads; i++) {
                    if (!new File(rootFile, "/Basecalling_Netcopy_complete_Read" + (i + 1) + ".txt").exists()) {
                      if (!new File(rootFile, "/Basecalling_Netcopy_complete_READ" + (i + 1) + ".txt").exists()) {
                        log.debug(countStr + runName + " :: No Basecalling_Netcopy_complete_Read" + (i + 1) + ".txt / Basecalling_Netcopy_complete_READ" + (i + 1) + ".txt!");
                        complete = false;
                        break;
                      }
                    }
                  }
                }

                run.put("sequencerName", runInfoDoc.getElementsByTagName("Instrument").item(0).getTextContent());

                if (runInfoDoc.getElementsByTagName("FlowcellId").getLength() != 0) {
                  run.put("containerlId", runInfoDoc.getElementsByTagName("FlowcellId").item(0).getTextContent());
                }
                else if (runInfoDoc.getElementsByTagName("Flowcell").getLength() != 0) {
                  run.put("containerId", runInfoDoc.getElementsByTagName("Flowcell").item(0).getTextContent());
                }
              }

              if (runParameters.exists()) {
                run.put("runparams", SubmissionUtils.transform(runParameters));
                Document runParamDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                SubmissionUtils.transform(runParameters, runParamDoc);

                if (!run.has("containerId") && runParamDoc.getElementsByTagName("Barcode").getLength() != 0) {
                  run.put("containerId", runParamDoc.getElementsByTagName("Barcode").item(0).getTextContent());
                }
              }
              else if (otherRunParameters.exists()) {
                run.put("runparams", SubmissionUtils.transform(otherRunParameters));
                Document runParamDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                SubmissionUtils.transform(otherRunParameters, runParamDoc);

                if (!run.has("containerId") && runParamDoc.getElementsByTagName("Barcode").getLength() != 0) {
                  run.put("containerId", runParamDoc.getElementsByTagName("Barcode").item(0).getTextContent());
                }
              }

              checkDates(rootFile, run);

              if (complete) {
                if (!new File(rootFile, "/Basecalling_Netcopy_complete.txt").exists() &&
                    (lastCycleLogFile != null && !lastCycleLogFile.exists())) {
                  log.debug(countStr + runName + " :: All Basecalling_Netcopy_complete_ReadX.txt exist but Basecalling_Netcopy_complete.txt doesn't exist and last cycle log file doesn't exist. Run is unknown");
                  map.get("Unknown").add(run);
                }
                else if (new File(rootFile, "/Basecalling_Netcopy_complete.txt").exists() &&
                    (lastCycleLogFile != null && !lastCycleLogFile.exists())) {
                  log.debug(countStr + runName + " :: All Basecalling_Netcopy_complete_ReadX.txt exist and Basecalling_Netcopy_complete.txt exists but last cycle log file doesn't exist. Run is unknown");
                  map.get("Unknown").add(run);
                }
                else {
                  log.debug(countStr + runName + " :: All Basecalling_Netcopy_complete*.txt exist and last cycle log file exists. Run is complete");
                  map.get("Completed").add(run);
                }
              }
              else {
                if (!completeFile.exists()) {
                  if (!new File(rootFile, "/Basecalling_Netcopy_complete.txt").exists() &&
                      (lastCycleLogFile != null && !lastCycleLogFile.exists())) {
                    log.debug(countStr + runName + " :: A Basecalling_Netcopy_complete_ReadX.txt doesn't exist and last cycle log file doesn't exist. Run is not complete");
                    map.get("Running").add(run);
                  }
                  else {
                    log.debug(countStr + runName + " :: Basecalling_Netcopy_complete*.txt don't exist and last cycle log file doesn't exist. Run is unknown");
                    map.get("Unknown").add(run);
                  }
                }
                else {
                  log.debug(countStr + runName + " :: Basecalling_Netcopy_complete*.txt don't exist and last cycle log file doesn't exist, but RTAComplete.txt exists. Run is complete");
                  map.get("Completed").add(run);
                }
              }
            }
            else if (oldStatusFile.exists()) {
              if (oldStatusFile.canRead()) {
                Document statusDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                SubmissionUtils.transform(oldStatusFile, statusDoc);

                runName = statusDoc.getElementsByTagName("RunName").item(0).getTextContent();
                run.put("runName", runName);

                run.put("status", SubmissionUtils.transform(oldStatusFile));
              }
              else {
                run.put("status", "<error><RunName>"+runName+"</RunName><ErrorMessage>Cannot read status file</ErrorMessage></error>");
              }

              if (runInfo.exists() && runInfo.canRead()) {
                run.put("runinfo", SubmissionUtils.transform(runInfo));

                Document runInfoDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                SubmissionUtils.transform(runInfo, runInfoDoc);

                if (!run.has("sequencerName")) {
                  run.put("sequencerName", runInfoDoc.getElementsByTagName("Instrument").item(0).getTextContent());
                }

                if (runInfoDoc.getElementsByTagName("FlowcellId").getLength() != 0) {
                  run.put("containerId", runInfoDoc.getElementsByTagName("FlowcellId").item(0).getTextContent());
                }
                else if (runInfoDoc.getElementsByTagName("Flowcell").getLength() != 0) {
                  run.put("containerId", runInfoDoc.getElementsByTagName("Flowcell").item(0).getTextContent());
                }
              }

              if (runParameters.exists() && runParameters.canRead()) {
                run.put("runparams", SubmissionUtils.transform(runParameters));
                Document runParamDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                SubmissionUtils.transform(runParameters, runParamDoc);

                if (!run.has("sequencerName")) {
                  run.put("sequencerName", runParamDoc.getElementsByTagName("ScannerID").item(0).getTextContent());
                }

                if (!run.has("containerId") && runParamDoc.getElementsByTagName("Barcode").getLength() != 0) {
                  run.put("containerId", runParamDoc.getElementsByTagName("Barcode").item(0).getTextContent());
                }
              }

              if (!completeFile.exists()) {
                checkDates(rootFile, run);

                if (run.has("completionDate")) {
                  log.debug(countStr + runName + " :: Completed");
                  map.get("Completed").add(run);
                }
                else {
                  log.debug(countStr + runName + " :: Running");
                  map.get("Running").add(run);
                }
              }
              else {
                checkDates(rootFile, run);

                log.debug(countStr + runName + " :: Completed");
                map.get("Completed").add(run);
              }
            }
            else if (newStatusFile.exists()) {
              if (newStatusFile.canRead()) {
                Document statusDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                SubmissionUtils.transform(newStatusFile, statusDoc);

                runName = statusDoc.getElementsByTagName("RunName").item(0).getTextContent();
                run.put("runName", runName);

                int numReads = new Integer(statusDoc.getElementsByTagName("NumberOfReads").item(0).getTextContent());
                int numCycles = new Integer(statusDoc.getElementsByTagName("NumCycles").item(0).getTextContent());
                int imgCycle = new Integer(statusDoc.getElementsByTagName("ImgCycle").item(0).getTextContent());
                int scoreCycle = new Integer(statusDoc.getElementsByTagName("ScoreCycle").item(0).getTextContent());
                int callCycle = new Integer(statusDoc.getElementsByTagName("CallCycle").item(0).getTextContent());

                run.put("numCycles", numCycles);

                if (!new File(rootFile, "/Basecalling_Netcopy_complete_SINGLEREAD.txt").exists()) {
                  for (int i = 0; i < numReads; i++) {
                    if (!new File(rootFile, "/Basecalling_Netcopy_complete_Read" + (i + 1) + ".txt").exists()) {
                      if (!new File(rootFile, "/Basecalling_Netcopy_complete_READ" + (i + 1) + ".txt").exists()) {
                        log.debug(countStr + runName + " :: No Basecalling_Netcopy_complete_Read" + (i + 1) + ".txt / Basecalling_Netcopy_complete_READ" + (i + 1) + ".txt!");
                        complete = false;
                        break;
                      }
                    }
                  }
                }

                run.put("status", SubmissionUtils.transform(newStatusFile));

                if (runInfo.exists()) {
                  run.put("runinfo", SubmissionUtils.transform(runInfo));

                  Document runInfoDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                  SubmissionUtils.transform(runInfo, runInfoDoc);

                  if (!run.has("sequencerName")) {
                    run.put("sequencerName", runInfoDoc.getElementsByTagName("Instrument").item(0).getTextContent());
                  }

                  if (runInfoDoc.getElementsByTagName("FlowcellId").getLength() != 0) {
                    run.put("containerId", runInfoDoc.getElementsByTagName("FlowcellId").item(0).getTextContent());
                  }
                  else if (runInfoDoc.getElementsByTagName("Flowcell").getLength() != 0) {
                    run.put("containerId", runInfoDoc.getElementsByTagName("Flowcell").item(0).getTextContent());
                  }
                }

                if (runParameters.exists()) {
                  run.put("runparams", SubmissionUtils.transform(runParameters));
                  Document runParamDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                  SubmissionUtils.transform(runParameters, runParamDoc);

                  if (!run.has("sequencerName")) {
                    run.put("sequencerName", runParamDoc.getElementsByTagName("ScannerID").item(0).getTextContent());
                  }

                  if (!run.has("containerId") && runParamDoc.getElementsByTagName("Barcode").getLength() != 0) {
                    run.put("containerId", runParamDoc.getElementsByTagName("Barcode").item(0).getTextContent());
                  }
                }

                checkDates(rootFile, run);

                if (complete) {
                  if (!new File(rootFile, "/Basecalling_Netcopy_complete.txt").exists() &&
                      (numCycles != imgCycle || numCycles != scoreCycle || numCycles != callCycle)) {
                    log.debug(countStr + runName + " :: All Basecalling_Netcopy_complete_ReadX.txt exist but Basecalling_Netcopy_complete.txt doesn't exist and cycles don't match. Run is unknown");
                    map.get("Unknown").add(run);
                  }
                  else if (new File(rootFile, "/Basecalling_Netcopy_complete.txt").exists() &&
                      (numCycles != imgCycle || numCycles != scoreCycle || numCycles != callCycle)) {
                    log.debug(countStr + runName + " :: All Basecalling_Netcopy_complete_ReadX.txt exist and Basecalling_Netcopy_complete.txt exists but cycles don't match. Run is unknown");
                    map.get("Unknown").add(run);
                  }
                  else {
                    log.debug(countStr + runName + " :: All Basecalling_Netcopy_complete*.txt exist and cycles match. Run is complete");
                    map.get("Completed").add(run);
                  }
                }
                else {
                  if (!completeFile.exists()) {
                    if (!new File(rootFile, "/Basecalling_Netcopy_complete.txt").exists() &&
                        (numCycles != imgCycle || numCycles != scoreCycle || numCycles != callCycle)) {
                      log.debug(countStr + runName + " :: A Basecalling_Netcopy_complete_ReadX.txt doesn't exist and cycles don't match. Run is not complete");
                      map.get("Running").add(run);
                    }
                    else {
                      log.debug(countStr + runName + " :: Basecalling_Netcopy_complete*.txt don't exist and cycles don't match. Run is unknown");
                      map.get("Unknown").add(run);
                    }
                  }
                  else {
                    log.debug(countStr + runName + " :: Basecalling_Netcopy_complete*.txt don't exist and cycles don't match, but Run.completed exists. Run is complete");
                    map.get("Completed").add(run);
                  }
                }
              }
              else {
                run.put("status", "<error><RunName>"+runName+"</RunName><ErrorMessage>Cannot read status file</ErrorMessage></error>");

                checkDates(rootFile, run);

                if (!completeFile.exists()) {
                  if (!new File(rootFile, "/Basecalling_Netcopy_complete.txt").exists()) {
                    log.debug(countStr + runName + " :: Cannot read Status.xml and Basecalling_Netcopy_complete.txt/Run.completed doesn't exist. Run is unknown");
                    map.get("Unknown").add(run);
                  }
                  else {
                    map.get("Completed").add(run);
                    log.debug(countStr + runName + " :: Cannot read Status.xml and Basecalling_Netcopy_complete.txt exists. Run is unknown");
                  }
                }
                else {
                  log.debug(countStr + runName + " :: Basecalling_Netcopy_complete.txt exists and Run.completed exists. Run is complete");
                  map.get("Completed").add(run);
                }
              }
            }
            else {
              run.put("status", "<error><RunName>"+runName+"</RunName><ErrorMessage>No status file exists</ErrorMessage></error>");

              checkDates(rootFile, run);

              if (!completeFile.exists()) {
                if (!new File(rootFile, "/Basecalling_Netcopy_complete.txt").exists()) {
                  log.debug(countStr + runName + " :: Status.xml doesn't exist and Basecalling_Netcopy_complete.txt/Run.completed doesn't exist. Run is unknown");
                  map.get("Unknown").add(run);
                }
                else {
                  log.debug(countStr + runName + " :: Status.xml doesn't exist but Basecalling_Netcopy_complete.txt exists. Run is completed");
                  map.get("Completed").add(run);
                }
              }
              else {
                log.debug(countStr + runName + " :: Basecalling_Netcopy_complete.txt exists and Run.completed exists. Run is complete");
                map.get("Completed").add(run);
              }
            }
          }
          catch (ParserConfigurationException e) {
            //e.printStackTrace();
            log.error("Error configuring parser: " + e.getMessage());
          }
          catch (TransformerException e) {
            //e.printStackTrace();
            log.error("Error transforming XML: " + e.getMessage());
          }
          catch (IOException e) {
            //e.printStackTrace();
            log.error("Error with file IO: " + e.getMessage());
          }
        }
        else {
          log.error(rootFile.getName() + " :: Permission denied");
        }
      }
    }

    HashMap<String, String> smap = new HashMap<String, String>();
    for (String key : map.keySet()) {
      smap.put(key, map.get(key).toString());
    }

    return smap;
  }

  private void checkDates(File rootFile, JSONObject run) throws IOException {
    String runName = run.getString("runName");

    String runDirRegex = "(\\d{6})_[A-z0-9]+_\\d+_[A-z0-9_\\+\\-]*";
    Matcher startMatcher = Pattern.compile(runDirRegex).matcher(runName);
    if (startMatcher.matches()) {
      log.debug(runName + " :: Got start date -> " + startMatcher.group(1));
      run.put("startDate", startMatcher.group(1));
    }

    File cycleTimeLog = new File(rootFile, "/Logs/CycleTimes.txt");
    File rtaLog = new File(rootFile, "/Data/RTALogs/Log.txt");
    File rtaLog2 = new File(rootFile, "/Data/Log.txt");
    File eventsLog = new File(rootFile, "/Events.log");
    File rtaComplete = new File(rootFile, "/RTAComplete.txt");

    if (rtaLog.exists() && rtaLog.canRead()) {
      Matcher m = LimsUtils.tailGrep(rtaLog, runCompleteLogPattern, 10);
      if (m != null && m.groupCount() > 0) {
        log.debug(runName + " :: Got RTALogs Log.txt completion date -> " + m.group(1));
        run.put("completionDate", m.group(1));
      }
    }
    else if (rtaLog2.exists() && rtaLog2.canRead()) {
      Matcher m = LimsUtils.tailGrep(rtaLog2, runCompleteLogPattern, 10);
      if (m != null && m.groupCount() > 0) {
        log.debug(runName + " :: Got Log.txt completion date -> " + m.group(1));
        run.put("completionDate", m.group(1));
      }
    }

    if (run.has("numCycles") && cycleTimeLog.exists() && cycleTimeLog.canRead()) {
      int numCycles = run.getInt("numCycles");
      Pattern p = Pattern.compile(
        "(\\d{1,2}\\/\\d{1,2}\\/\\d{4})\\s+(\\d{2}:\\d{2}:\\d{2})\\.\\d{3}\\s+[A-z0-9]+\\s+" + numCycles + "\\s+End\\s{1}Imaging"
      );

      Matcher m = LimsUtils.tailGrep(cycleTimeLog, p, 10);
      if (m != null && m.groupCount() > 0) {
        String cycleDateStr = m.group(1) + "," + m.group(2);
        if (run.has("completionDate")) {
          log.debug(runName + " :: Checking " + cycleDateStr + " vs. " + run.getString("completionDate"));
          try {
            Date cycleDate = logDateFormat.parse(cycleDateStr);
            Date cDate = logDateFormat.parse(run.getString("completionDate"));

            if (cycleDate.after(cDate)) {
              log.debug(runName + " :: Cycletimes completion date is newer -> " + cycleDateStr);
              run.put("completionDate", cycleDateStr);
            }
          }
          catch (ParseException e) {
            log.debug(runName + " :: Oops. Can't parse dates. Falling back!");
          }
        }
      }
    }

    if (!run.has("completionDate")) {
      //attempt to get latest log file entry date
      if (rtaLog.exists() && rtaLog.canRead()) {
        Matcher m = LimsUtils.tailGrep(rtaLog, lastDateEntryLogPattern, 1);
        if (m != null && m.groupCount() > 0) {
          log.debug(runName + " :: Got RTALogs Log.txt last entry date -> " + m.group(1));
          run.put("completionDate", m.group(1));
        }
      }
      else if (rtaLog2.exists() && rtaLog2.canRead()) {
        Matcher m = LimsUtils.tailGrep(rtaLog2, lastDateEntryLogPattern, 1);
        if (m != null && m.groupCount() > 0) {
          log.debug(runName + " :: Got Log.txt last entry date -> " + m.group(1));
          run.put("completionDate", m.group(1));
        }
      }

      if (run.has("numCycles") && cycleTimeLog.exists() && cycleTimeLog.canRead()) {
        int numCycles = run.getInt("numCycles");
        Pattern p = Pattern.compile(
                "(\\d{1,2}\\/\\d{1,2}\\/\\d{4})\\s+(\\d{2}:\\d{2}:\\d{2})\\.\\d{3}\\s+[A-z0-9]+\\s+" + numCycles + "\\s+End\\s{1}Imaging"
        );

        Matcher m = LimsUtils.tailGrep(cycleTimeLog, p, 10);
        if (m != null && m.groupCount() > 0) {
          log.debug(runName + " :: Got cycletimes last entry date -> " + m.group(1) + "," + m.group(2));
          String cycleDateStr = m.group(1) + "," + m.group(2);
          if (run.has("completionDate")) {
            log.debug(runName + " :: Checking " + cycleDateStr + " vs. " + run.getString("completionDate"));
            try {
              Date cycleDate = logDateFormat.parse(cycleDateStr);
              Date cDate = logDateFormat.parse(run.getString("completionDate"));

              if (cycleDate.after(cDate)) {
                log.debug(runName + " :: Cycletimes completion date is newer -> " + cycleDateStr);
                run.put("completionDate", cycleDateStr);
              }
            }
            catch (ParseException e) {
              log.debug(runName + " :: Oops. Can't parse dates. Falling back!");
            }
          }
        }
      }
    }

    //still nothing? attempt with Events.log
    if (!run.has("completionDate")) {
      //attempt to get latest log file entry date
      if (eventsLog.exists() && eventsLog.canRead()) {
        log.debug(runName + " :: Checking events log...");
        Pattern p = Pattern.compile(
                "\\.*\\s+(\\d{1,2}\\/\\d{2}\\/\\d{4})\\s+(\\d{1,2}:\\d{2}:\\d{2}).\\d+.*"
        );

        Matcher m = LimsUtils.tailGrep(eventsLog, p, 50);
        if (m != null && m.groupCount() > 0) {
          log.debug(runName + " :: Got last log event date -> " + m.group(1)+","+m.group(2));
          run.put("completionDate", m.group(1)+","+m.group(2));
        }
      }
    }

    // last ditch attempt with RTAComplete.txt
    if (!run.has("completionDate")) {
      if (rtaComplete.exists() && rtaComplete.canRead()) {
        log.debug(runName + " :: Last ditch attempt. Checking RTAComplete log...");
        Pattern p = Pattern.compile(
                "\\.*(\\d{1,2}\\/\\d{1,2}\\/\\d{4}),(\\d{1,2}:\\d{1,2}:\\d{1,2}).\\d+.*"
        );

        Matcher m = LimsUtils.tailGrep(rtaComplete, p, 2);
        if (m != null && m.groupCount() > 0) {
          log.debug(runName + " :: Got RTAComplete date -> " + m.group(1)+","+m.group(2));
          run.put("completionDate", m.group(1)+","+m.group(2));
        }
      }
    }

    if (!run.has("completionDate")) {
      run.put("completionDate", "null");
    }
  }

  public byte[] transformToJson(Set<File> files) {
    Map<String, String> smap = transform(files);
    JSONObject json = new JSONObject();
    for (String key : smap.keySet()) {
      json.put(key, JSONArray.fromObject(smap.get(key)));
    }
    return (json.toString() + "\r\n").getBytes();
  }

  public Message<Set<String>> runStatusFilesToStringSetMessage(Message<Set<File>> message) {
    Set<File> files = message.getPayload();
    Set<String> xmls = new HashSet<String>();
    for (File f : files) {
      try {
        xmls.add(SubmissionUtils.transform(f));
      }
      catch (TransformerException e) {
        //e.printStackTrace();
        log.error("Error transforming XML: " + e.getMessage());
      }
      catch (IOException e) {
        //e.printStackTrace();
        log.error("Error with file IO: " + e.getMessage());
      }
    }
    return NotificationUtils.buildSimpleMessage(xmls);
  }

  public Message<Set<String>> runCompletedFilesToStringSetMessage(Message<Set<File>> message) {
    Set<File> files = message.getPayload();
    Set<String> runNames = new HashSet<String>();
    String regex = ".*/([\\d]+_[A-z0-9]+_[\\d]+_[A-z0-9_]*)[/]{0,1}.*";
    Pattern p = Pattern.compile(regex);
    for (File f : files) {
      Matcher m = p.matcher(f.getAbsolutePath());
      if (m.matches()) {
        runNames.add(m.group(1));
      }
    }
    return NotificationUtils.buildSimpleMessage(runNames);
  }

  public Set<String> runStatusJobToStringSet(JobExecution exec) {
    Set<String> files = new HashSet<String>();
    for (Map.Entry<String, JobParameter> params : exec.getJobInstance().getJobParameters().getParameters().entrySet()) {
      File f = new File(params.getValue().toString());
      try {
        files.add(SubmissionUtils.transform(f));
      }
      catch (TransformerException e) {
        //e.printStackTrace();
        log.error("Error transforming XML: " + e.getMessage());
      }
      catch (IOException e) {
        //e.printStackTrace();
        log.error("Error with file IO: " + e.getMessage());
      }
    }
    return files;
  }

  public Set<String> runCompletedJobToStringSet(JobExecution exec) {
    Set<String> runNames = new HashSet<String>();
    for (Map.Entry<String, JobParameter> params : exec.getJobInstance().getJobParameters().getParameters().entrySet()) {
      runNames.add(params.getKey());
    }
    return runNames;
  }
}
