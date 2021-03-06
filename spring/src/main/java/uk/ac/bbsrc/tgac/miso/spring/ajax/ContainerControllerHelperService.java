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

package uk.ac.bbsrc.tgac.miso.spring.ajax;

import com.eaglegenomics.simlims.core.User;
import com.eaglegenomics.simlims.core.manager.SecurityManager;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sourceforge.fluxion.ajax.Ajaxified;
import net.sourceforge.fluxion.ajax.util.JSONUtils;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.ac.bbsrc.tgac.miso.core.data.*;
import uk.ac.bbsrc.tgac.miso.core.data.type.PlatformType;
import uk.ac.bbsrc.tgac.miso.core.exception.MalformedExperimentException;
import uk.ac.bbsrc.tgac.miso.core.factory.DataObjectFactory;
import uk.ac.bbsrc.tgac.miso.core.manager.RequestManager;
import uk.ac.bbsrc.tgac.miso.core.util.LimsUtils;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: davey
 * Date: 25-May-2010
 * Time: 16:39:52
 */
@Ajaxified
public class ContainerControllerHelperService {
  protected static final Logger log = LoggerFactory.getLogger(ContainerControllerHelperService.class);
  @Autowired
  private SecurityManager securityManager;
  @Autowired
  private RequestManager requestManager;
  @Autowired
  private DataObjectFactory dataObjectFactory;

  public JSONObject getPlatformTypes(HttpSession session, JSONObject json) throws IOException {
    StringBuilder b = new StringBuilder();
    List<String> platformTypes = PlatformType.getKeys();
    for (String p : platformTypes) {
      b.append("<input type='radio' name='platformTypes' id='platformTypes" + p + "' value='" + p + "' onchange='Container.ui.changeContainerPlatformType(this);'/>");
      b.append("<label for='platformTypes" + p + "'>" + p + "</label>");
    }
    return JSONUtils.JSONObjectResponse("html", b.toString());
  }

  public JSONObject changePlatformType(HttpSession session, JSONObject json) {
    String newContainerType = json.getString("platformtype");
    PlatformType pt = PlatformType.get(newContainerType);
    String cId = json.getString("container_cId");
    try {
      User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());

      Map<String, Object> responseMap = new HashMap<String, Object>();
      if (pt != null) {
        SequencerPartitionContainer<SequencerPoolPartition> lf = dataObjectFactory.getSequencerPartitionContainer(pt, user);
        session.setAttribute("container_" + cId, lf);

        StringBuilder srb = new StringBuilder();
        srb.append("<select name='sequencer' id='sequencerReference' onchange='Container.ui.populateContainerOptions(this);'>");
        srb.append("<option value='0' selected='selected'>Please select...</option>");
        for (SequencerReference sr : requestManager.listSequencerReferencesByPlatformType(pt)) {
          srb.append("<option value='" + sr.getId() + "'>" + sr.getName() + " (" + sr.getPlatform().getInstrumentModel() + ")</option>");
        }
        srb.append("</select>");

        responseMap.put("sequencers", srb.toString());
      }
      else {
        return JSONUtils.SimpleJSONError("Unrecognised PlatformType");
      }
      return JSONUtils.JSONObjectResponse(responseMap);
    }
    catch (IOException e) {
      log.debug("Failed to change PlatformType", e);
      return JSONUtils.SimpleJSONError("Failed to change PlatformType");
    }
  }

  public JSONObject populateContainerOptions(HttpSession session, JSONObject json) {
    Long sequencerReferenceId = json.getLong("sequencerReference");
    try {
      SequencerReference sr = requestManager.getSequencerReferenceById(sequencerReferenceId);
      Map<String, Object> responseMap = new HashMap<String, Object>();
      responseMap.put("partitions", getContainerOptions(sr));
      return JSONUtils.JSONObjectResponse(responseMap);
    }
    catch (IOException e) {
      e.printStackTrace();
      return JSONUtils.SimpleJSONError("Failed to get Container options");
    }
  }

  private String getContainerOptions(SequencerReference sr) throws IOException {
    StringBuilder b = new StringBuilder();
    b.append("<span id='containerspan'>Containers: ");
    PlatformType pt = sr.getPlatform().getPlatformType();
    for (int i = 0; i < sr.getPlatform().getNumContainers(); i++) {
      b.append("<input id='container" + (i + 1) + "' name='containerselect' onchange='Container.ui.changeContainer(" + sr.getPlatform().getNumContainers() + ", \"" + pt.getKey() + "\", " + sr.getId() + ");' type='radio' value='" + (i + 1) + "'/>" + (i + 1));
    }
    b.append("</span><br/>");
    b.append("<div id='containerdiv' class='note ui-corner-all'> </div>");
    return b.toString();
  }

  public JSONObject changeContainer(HttpSession session, JSONObject json) {
    if (json.has("platform")) {
      String platform = json.getString("platform");
      PlatformType pt = PlatformType.get(platform);
      if (pt != null) {
        if (pt.equals(PlatformType.ILLUMINA)) {
          return changeIlluminaContainer(session, json);
        }
        else if (pt.equals(PlatformType.LS454)) {
          return changeLS454Container(session, json);
        }
        else if (pt.equals(PlatformType.SOLID)) {
          return changeSolidContainer(session, json);
        }
        /*
        else if (pt.equals(PlatformType.IONTORRENT)) {
          return null;
        }
        */
        else if (pt.equals(PlatformType.PACBIO)) {
          return changePacBioContainer(session, json);
        }
        else {
          return JSONUtils.SimpleJSONError("Unsupported platform type: " + platform);
        }
      }
    }
    return JSONUtils.SimpleJSONError("No platform specified");
  }

  public JSONObject changeIlluminaContainer(HttpSession session, JSONObject json) {
    long seqRefId = json.getLong("sequencerReferenceId");
    StringBuilder b = new StringBuilder();
    b.append("<h2>Container</h2>");
    b.append("<table class='in'>");
    b.append("<tr><td>ID:</td><td><button onclick='Container.lookupContainer(this);' type='button' class='right-button ui-state-default ui-corner-all'>Lookup</button><div style='overflow:hidden'><input type='text' id='identificationBarcode' name='identificationBarcode'/><input type='hidden' value='on' name='_identificationBarcode'></div></td></tr>");
    b.append("<tr><td>Location:</td><td><input type='text' id='locationBarcode' name='locationBarcode'/><input type='hidden' value='on' name='_locationBarcode'></td></tr>");
    b.append("<tr><td>Validation:</td><td><input type='text' id='validationBarcode' name='validationBarcode'/><input type='hidden' value='on' name='_validationBarcode'></td></tr>");
    b.append("<tr><td>Paired:</td><td><input type='checkbox' id='paired' name='paired' value='false'/><input type='hidden' value='on' name='_paired'></td></tr>");
    b.append("</table>");
    b.append("<div id='partitionErrorDiv'> </div>");
    b.append("<div id='partitionDiv'>");

    try {
      SequencerReference sr = requestManager.getSequencerReferenceById(seqRefId);
      if ("Illumina MiSeq".equals(sr.getPlatform().getInstrumentModel())) {
        b.append("<i class='italicInfo'>Click in a partition box to beep/type in barcodes, or double click a pool on the right to sequentially add pools to the container</i>");
        b.append("<table class='in'>");
        b.append("<th>Lane No.</th>");
        b.append("<th>Pool</th>");

        b.append("<tr><td>1 </td><td width='90%'><div id='p_div-0'><ul class='runPartitionDroppable' bind='partitions[0].pool' partition='0' ondblclick='Container.partition.populatePartition(this);'></ul></div></td></tr>");
        b.append("</table>");
        b.append("</div>");

        SequencerPartitionContainer<SequencerPoolPartition> lf =
                (SequencerPartitionContainer<SequencerPoolPartition>) session.getAttribute("container_" + json.getString("container_cId"));
        lf.setPartitionLimit(1);
        lf.initEmptyPartitions();
      }
      else {
        b.append("<i class='italicInfo'>Click in a partition box to beep/type in barcodes, or double click a pool on the right to sequentially add pools to the container</i>");
        b.append("<table class='in'>");
        b.append("<th>Lane No.</th>");
        b.append("<th>Pool</th>");

        b.append("<tr><td>1 </td><td width='90%'><div id='p_div-0'><ul class='runPartitionDroppable' bind='partitions[0].pool' partition='0' ondblclick='Container.partition.populatePartition(this);'></ul></div></td></tr>");
        b.append("<tr><td>2 </td><td width='90%'><div id='p_div-1'><ul class='runPartitionDroppable' bind='partitions[1].pool' partition='1' ondblclick='Container.partition.populatePartition(this);'></ul></div></td></tr>");
        b.append("<tr><td>3 </td><td width='90%'><div id='p_div-2'><ul class='runPartitionDroppable' bind='partitions[2].pool' partition='2' ondblclick='Container.partition.populatePartition(this);'></ul></div></td></tr>");
        b.append("<tr><td>4 </td><td width='90%'><div id='p_div-3'><ul class='runPartitionDroppable' bind='partitions[3].pool' partition='3' ondblclick='Container.partition.populatePartition(this);'></ul></div></td></tr>");
        b.append("<tr><td>5 </td><td width='90%'><div id='p_div-4'><ul class='runPartitionDroppable' bind='partitions[4].pool' partition='4' ondblclick='Container.partition.populatePartition(this);'></ul></div></td></tr>");
        b.append("<tr><td>6 </td><td width='90%'><div id='p_div-5'><ul class='runPartitionDroppable' bind='partitions[5].pool' partition='5' ondblclick='Container.partition.populatePartition(this);'></ul></div></td></tr>");
        b.append("<tr><td>7 </td><td width='90%'><div id='p_div-6'><ul class='runPartitionDroppable' bind='partitions[6].pool' partition='6' ondblclick='Container.partition.populatePartition(this);'></ul></div></td></tr>");
        b.append("<tr><td>8 </td><td width='90%'><div id='p_div-7'><ul class='runPartitionDroppable' bind='partitions[7].pool' partition='7' ondblclick='Container.partition.populatePartition(this);'></ul></div></td></tr>");
        b.append("</table>");
        b.append("</div>");

        SequencerPartitionContainer<SequencerPoolPartition> lf =
                (SequencerPartitionContainer<SequencerPoolPartition>) session.getAttribute("container_" + json.getString("container_cId"));
        lf.setPartitionLimit(8);
        lf.initEmptyPartitions();
      }
      b.append("<div id='containerdiv0'> </div>");
      b.append("</div>");
      return JSONUtils.SimpleJSONResponse(b.toString());
    }
    catch (IOException e) {
      e.printStackTrace();
      return JSONUtils.SimpleJSONError("No sequencer reference defined");
    }
  }

  public JSONObject changeLS454Container(HttpSession session, JSONObject json) {
    StringBuilder b = new StringBuilder();
    b.append("<h2>Container</h2>");
    b.append("<table class='in'>");
    b.append("<tr><td>ID:</td><td><input type='text' id='identificationBarcode' name='identificationBarcode'/><input type='hidden' value='on' name='_identificationBarcode'><button onclick='Container.lookupContainer(this);' type='button' class='right-button ui-state-default ui-corner-all'>Lookup</button></td></tr>");
    b.append("<tr><td>Location:</td><td><input type='text' id='locationBarcode' name='locationBarcode'/><input type='hidden' value='on' name='_locationBarcode'></td></tr>");
    b.append("<tr><td>Validation:</td><td><input type='text' id='validationBarcode' name='validationBarcode'/><input type='hidden' value='on' name='_validationBarcode'></td></tr>");
    b.append("<tr><td>Paired:</td><td><input type='checkbox' id='paired' name='paired'/><input type='hidden' value='on' name='_paired'></td></tr>");
    b.append("</table>");
    b.append("<div id='partitionErrorDiv'> </div>");
    b.append("<div id='partitionDiv'>");
    b.append("<input id='chamber1' name='container0Select' onchange='Container.ui.changeContainerLS454Chamber(this, 0);' type='radio' value='1'/>1 ");
    b.append("<input id='chamber2' name='container0Select' onchange='Container.ui.changeContainerLS454Chamber(this, 0);' type='radio' value='2'/>2 ");
    b.append("<input id='chamber4' name='container0Select' onchange='Container.ui.changeContainerLS454Chamber(this, 0);' type='radio' value='4'/>4 ");
    b.append("<input id='chamber8' name='container0Select' onchange='Container.ui.changeContainerLS454Chamber(this, 0);' type='radio' value='8'/>8 ");
    b.append("<input id='chamber16' name='container0Select' onchange='Container.ui.changeContainerLS454Chamber(this, 0);' type='radio' value='16'/>16<br/>");
    b.append("<div id='containerdiv0'> </div>");
    b.append("</div>");
    return JSONUtils.SimpleJSONResponse(b.toString());
  }

  public JSONObject changeSolidContainer(HttpSession session, JSONObject json) {
    long seqRefId = json.getLong("sequencerReferenceId");
    StringBuilder b = new StringBuilder();
    try {
      SequencerReference sr = requestManager.getSequencerReferenceById(seqRefId);
      b.append("<h2>Container</h2>");
      b.append("<table class='in'>");
      b.append("<tr><td>ID:</td><td><button onclick='Container.lookupContainer(this);' type='button' class='right-button ui-state-default ui-corner-all'>Lookup</button><div style='overflow:hidden'><input type='text' id='identificationBarcode' name='identificationBarcode'/><input type='hidden' value='on' name='_identificationBarcode'></div></td></tr>");
      b.append("<tr><td>Location:</td><td><input type='text' id='locationBarcode' name='locationBarcode'/><input type='hidden' value='on' name='_locationBarcode'></td></tr>");
      b.append("<tr><td>Validation:</td><td><input type='text' id='validationBarcode' name='validationBarcode'/><input type='hidden' value='on' name='_validationBarcode'></td></tr>");
      b.append("<tr><td>Paired:</td><td><input type='checkbox' id='paired' name='paired'/><input type='hidden' value='on' name='_paired'></td></tr>");
      b.append("</table>");
      b.append("<div id='partitionErrorDiv'> </div>");
      b.append("<div id='partitionDiv'>");
      if ("AB SOLiD 5500xl".equals(sr.getPlatform().getInstrumentModel())) {
        b.append("<table class='in'>");
        b.append("<th>Chamber No.</th>");
        b.append("<th>Pool</th>");

        b.append("<tr><td>1 </td><td width='90%'><div id='p_div_0-0' class='barcodeEntryDiv'><ul class='runPartitionDroppable' bind='partitions[0].pool' partition='0' ondblclick='Container.partition.populatePartition(this);'></ul></div></td></tr>");
        b.append("<tr><td>2 </td><td width='90%'><div id='p_div_0-1' class='barcodeEntryDiv'><ul class='runPartitionDroppable' bind='partitions[1].pool' partition='1' ondblclick='Container.partition.populatePartition(this);'></ul></div></td></tr>");
        b.append("<tr><td>3 </td><td width='90%'><div id='p_div_0-2' class='barcodeEntryDiv'><ul class='runPartitionDroppable' bind='partitions[2].pool' partition='2' ondblclick='Container.partition.populatePartition(this);'></ul></div></td></tr>");
        b.append("<tr><td>4 </td><td width='90%'><div id='p_div_0-3' class='barcodeEntryDiv'><ul class='runPartitionDroppable' bind='partitions[3].pool' partition='3' ondblclick='Container.partition.populatePartition(this);'></ul></div></td></tr>");
        b.append("<tr><td>5 </td><td width='90%'><div id='p_div_0-4' class='barcodeEntryDiv'><ul class='runPartitionDroppable' bind='partitions[4].pool' partition='4' ondblclick='Container.partition.populatePartition(this);'></ul></div></td></tr>");
        b.append("<tr><td>6 </td><td width='90%'><div id='p_div_0-5' class='barcodeEntryDiv'><ul class='runPartitionDroppable' bind='partitions[5].pool' partition='5' ondblclick='Container.partition.populatePartition(this);'></ul></div></td></tr>");
        b.append("</table>");

        SequencerPartitionContainer<SequencerPoolPartition> lf =
                (SequencerPartitionContainer<SequencerPoolPartition>) session.getAttribute("container_" + json.getString("container_cId"));
        lf.setPartitionLimit(6);
        lf.initEmptyPartitions();
        session.setAttribute("container_" + json.getString("container_cId"), lf);
      }
      else {
        b.append("<input id='chamber1' name='container0Select' onchange='Container.ui.changeContainerSolidChamber(this, 0);' type='radio' value='1'/>1 ");
        b.append("<input id='chamber4' name='container0Select' onchange='Container.ui.changeContainerSolidChamber(this, 0);' type='radio' value='4'/>4 ");
        b.append("<input id='chamber8' name='container0Select' onchange='Container.ui.changeContainerSolidChamber(this, 0);' type='radio' value='8'/>8 ");
        b.append("<input id='chamber16' name='container0Select' onchange='Container.ui.changeContainerSolidChamber(this, 0);' type='radio' value='16'/>16<br/>");
      }
      b.append("<div id='containerdiv0'> </div>");
      b.append("</div>");
      return JSONUtils.SimpleJSONResponse(b.toString());
    }
    catch (IOException e) {
      e.printStackTrace();
      return JSONUtils.SimpleJSONError("No sequencer reference defined");
    }
  }

  public JSONObject changePacBioContainer(HttpSession session, JSONObject json) {
    StringBuilder b = new StringBuilder();
    b.append("<h2>Container</h2>");
    b.append("<table class='in'>");
    b.append("<tr><td>ID:</td><td><input type='text' id='identificationBarcode' name='identificationBarcode'/><input type='hidden' value='on' name='_identificationBarcode'><button onclick='Container.lookupContainer(this);' type='button' class='right-button ui-state-default ui-corner-all'>Lookup</button></td></tr>");
    b.append("<tr><td>Location:</td><td><input type='text' id='locationBarcode' name='locationBarcode'/><input type='hidden' value='on' name='_locationBarcode'></td></tr>");
    b.append("<tr><td>Validation:</td><td><input type='text' id='validationBarcode' name='validationBarcode'/><input type='hidden' value='on' name='_validationBarcode'></td></tr>");
    b.append("<tr><td>Paired:</td><td><input type='checkbox' id='paired' name='paired'/><input type='hidden' value='on' name='_paired'></td></tr>");
    b.append("</table>");
    b.append("<div id='partitionErrorDiv'> </div>");
    b.append("<div id='partitionDiv'>");
    b.append("<input id='chamber1' name='container0Select' onchange='Container.ui.changeContainerPacBioChamber(this, 0);' type='radio' value='1'/>1 ");
    b.append("<input id='chamber2' name='container0Select' onchange='Container.ui.changeContainerPacBioChamber(this, 0);' type='radio' value='2'/>2 ");
    b.append("<input id='chamber3' name='container0Select' onchange='Container.ui.changeContainerPacBioChamber(this, 0);' type='radio' value='3'/>3 ");
    b.append("<input id='chamber4' name='container0Select' onchange='Container.ui.changeContainerPacBioChamber(this, 0);' type='radio' value='4'/>4 ");
    b.append("<input id='chamber5' name='container0Select' onchange='Container.ui.changeContainerPacBioChamber(this, 0);' type='radio' value='5'/>5 ");
    b.append("<input id='chamber6' name='container0Select' onchange='Container.ui.changeContainerPacBioChamber(this, 0);' type='radio' value='6'/>6 ");
    b.append("<input id='chamber7' name='container0Select' onchange='Container.ui.changeContainerPacBioChamber(this, 0);' type='radio' value='7'/>7 ");
    b.append("<input id='chamber8' name='container0Select' onchange='Container.ui.changeContainerPacBioChamber(this, 0);' type='radio' value='8'/>8<br/>");

    b.append("<div id='containerdiv0'> </div>");
    b.append("</div>");
    return JSONUtils.SimpleJSONResponse(b.toString());
  }

  public JSONObject changeChamber(HttpSession session, JSONObject json) {
    if (json.has("platform")) {
      String platform = json.getString("platform");
      PlatformType pt = PlatformType.get(platform);
      if (pt != null) {
        if (pt.equals(PlatformType.LS454)) {
          return changeLS454Chamber(session, json);
        }
        else if (pt.equals(PlatformType.SOLID)) {
          return changeSolidChamber(session, json);
        }
        else if (pt.equals(PlatformType.PACBIO)) {
          return changePacBioChamber(session, json);
        }
        else {
          return JSONUtils.SimpleJSONError("Unrecognised platform type: " + platform);
        }
      }
    }
    return JSONUtils.SimpleJSONError("No platform specified");
  }

  public JSONObject changeLS454Chamber(HttpSession session, JSONObject json) {
    int numChambers = json.getInt("numChambers");
    int container = json.getInt("container");
    StringBuilder b = new StringBuilder();
    b.append("<i class='italicInfo'>Click in a partition box to beep/type in barcodes, or double click a pool on the right to sequentially add pools to the container</i>");
    b.append("<table class='in'>");
    b.append("<th>Chamber No.</th>");
    b.append("<th>Pool</th>");

    SequencerPartitionContainer<SequencerPoolPartition> lf =
            (SequencerPartitionContainer<SequencerPoolPartition>) session.getAttribute("container_" + json.getString("container_cId"));
    lf.setPartitionLimit(numChambers);
    lf.initEmptyPartitions();
    session.setAttribute("container_" + json.getString("container_cId"), lf);

    for (int i = 0; i < numChambers; i++) {
      b.append("<tr><td>" + (i + 1) + "</td>");
      b.append("<td width='90%'><div id='p_div_" + container + "-" + i + "' class='elementListDroppableDiv'><ul class='runPartitionDroppable' bind='partitions[" + i + "].pool' partition='" + i + "' ondblclick='Container.partition.populatePartition(this);'>");
      b.append("</ul></div></td>");
      b.append("</tr>");
    }
    b.append("</table>");
    return JSONUtils.SimpleJSONResponse(b.toString());
  }

  public JSONObject changeSolidChamber(HttpSession session, JSONObject json) {
    int numChambers = json.getInt("numChambers");
    int container = json.getInt("container");
    StringBuilder b = new StringBuilder();
    b.append("<i class='italicInfo'>Click in a partition box to beep/type in barcodes, or double click a pool on the right to sequentially add pools to the container</i>");
    b.append("<table class='in'>");
    b.append("<th>Chamber No.</th>");
    b.append("<th>Pool</th>");

    SequencerPartitionContainer<SequencerPoolPartition> lf =
            (SequencerPartitionContainer<SequencerPoolPartition>) session.getAttribute("container_" + json.getString("container_cId"));
    lf.setPartitionLimit(numChambers);
    lf.initEmptyPartitions();
    session.setAttribute("container_" + json.getString("container_cId"), lf);

    for (int i = 0; i < numChambers; i++) {
      b.append("<tr><td>" + (i + 1) + "</td>");
      b.append("<td width='90%'><div id='p_div_" + container + "-" + i + "' class='elementListDroppableDiv'><ul class='runPartitionDroppable' bind='partitions[" + i + "].pool' partition='" + i + "' ondblclick='Container.partition.populatePartition(this);'>");
      b.append("</ul></div></td>");
      b.append("</tr>");
    }
    b.append("</table>");
    return JSONUtils.SimpleJSONResponse(b.toString());
  }

  public JSONObject changePacBioChamber(HttpSession session, JSONObject json) {
    int numChambers = json.getInt("numChambers");
    int container = json.getInt("container");
    StringBuilder b = new StringBuilder();
    b.append("<i class='italicInfo'>Click in a partition box to beep/type in barcodes, or double click a pool on the right to sequentially add pools to the container</i>");
    b.append("<table class='in'>");
    b.append("<th>Chamber No.</th>");
    b.append("<th>Pool</th>");

    SequencerPartitionContainer<SequencerPoolPartition> lf =
            (SequencerPartitionContainer<SequencerPoolPartition>) session.getAttribute("container_" + json.getString("container_cId"));
    lf.setPartitionLimit(numChambers);
    lf.initEmptyPartitions();
    session.setAttribute("container_" + json.getString("container_cId"), lf);

    for (int i = 0; i < numChambers; i++) {
      b.append("<tr><td>" + (i + 1) + "</td>");
      b.append("<td width='90%'><div id='p_div_" + container + "-" + i + "' class='elementListDroppableDiv'><ul class='runPartitionDroppable' bind='partitions[" + i + "].pool' partition='" + i + "' ondblclick='Container.partition.populatePartition(this);'>");
      b.append("</tr>");
    }
    b.append("</table>");
    return JSONUtils.SimpleJSONResponse(b.toString());
  }

  public JSONObject getPoolByBarcode(HttpSession session, JSONObject json) {
    String barcode = json.getString("barcode");
    int partition = json.getInt("partition");

    try {
      if (barcode != null && !"".equals(barcode)) {
        if (LimsUtils.isBase64String(barcode)) {
          //Base64-encoded string, most likely a barcode image beeped in. decode and search
          barcode = new String(Base64.decodeBase64(barcode));
        }
      }

      Pool p = requestManager.getPoolByBarcode(barcode);
      SequencerPartitionContainer<SequencerPoolPartition> lf =
              (SequencerPartitionContainer<SequencerPoolPartition>) session.getAttribute("container_" + json.getString("container_cId"));
      if (lf.getPlatformType().equals(p.getPlatformType())) {
        return JSONUtils.JSONObjectResponse("html", poolHtml(p, partition));
      }
      else {
        return JSONUtils.JSONObjectResponse("err", "Error: pool platform does not match container platform");
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      return JSONUtils.JSONObjectResponse("err", "Error: no such pool");
    }
  }

  private String poolHtml(Pool<? extends Poolable> p, int partition) {
    StringBuilder b = new StringBuilder();
    try {
      b.append("<div style='position:relative' onMouseOver='this.className=\"dashboardhighlight\"' onMouseOut='this.className=\"dashboard\"' class='dashboard'>");
      b.append("<div style=\"float:left\"><b>" + p.getName() + " (" + p.getCreationDate() + ")</b><br/>");

      Collection<? extends Dilution> ds = p.getDilutions();
      for (Dilution d : ds) {
        b.append("<span>" + d.getName() + " (" + d.getLibrary().getSample().getProject().getAlias() + ")</span><br/>");
      }

      b.append("<br/><i>");
      Collection<Experiment> exprs = p.getExperiments();
      for (Experiment e : exprs) {
        b.append("<span>" + e.getStudy().getProject().getAlias() + "(" + e.getName() + ": " + p.getDilutions().size() + " dilutions)</span><br/>");
      }
      b.append("</i>");

      if (p.getExperiments().size() == 0) {
        Set<Project> pooledProjects = new HashSet<Project>();
        Collection<? extends Dilution> dils = p.getDilutions();
        for (Dilution d : dils) {
          pooledProjects.add(d.getLibrary().getSample().getProject());
        }

        b.append("<div style='float:left; clear:both'><div id='studySelectDiv" + partition + "'><select name='poolStudies" + partition + "' id='poolStudies" + partition + "'>");
        for (Project project : pooledProjects) {
          Collection<Study> studies = requestManager.listAllStudiesByProjectId(project.getProjectId());
          if (studies.isEmpty()) {
            throw new Exception("No studies available on project " + project.getName() + ". At least one study must be available for each project associated with this Pool.");
          }
          else {
            for (Study s : studies) {
              b.append("<option value='" + s.getId() + "'>" + s.getAlias() + " (" + s.getName() + " - " + s.getStudyType() + ")</option>");
            }
          }
        }
        b.append("</select>");
        b.append("<input id='studySelectButton-" + partition + "_" + p.getId() + "' type='button' onclick=\"Container.partition.selectContainerStudy('" + partition + "', " + p.getId() + ");\" class=\"ui-state-default ui-corner-all\" value='Select Study'/>");
        b.append("</div></div>");
      }

      b.append("<input type='hidden' name='partitions[" + partition + "].pool' id='pId" + p.getId() + "' value='" + p.getId() + "'/></div>");
      b.append("<div style='position: absolute; bottom: 0; right: 0; font-size: 24px; font-weight: bold; color:#BBBBBB'>" + p.getPlatformType().getKey() + "</div>");
      b.append("<span style='position: absolute; top: 0; right: 0;' onclick='Container.pool.confirmPoolRemove(this);' class='float-right ui-icon ui-icon-circle-close'></span>");
      b.append("</div>");
    }
    catch (IOException e) {
      e.printStackTrace();
      return "Cannot get studies for pool: " + e.getMessage();
    }
    catch (Exception e) {
      e.printStackTrace();
      return "Cannot get studies for pool: " + e.getMessage();
    }

    return b.toString();
  }

  public JSONObject selectStudyForPool(HttpSession session, JSONObject json) {
    try {
      Long poolId = json.getLong("poolId");
      Pool p = requestManager.getPoolById(poolId);

      Long studyId = json.getLong("studyId");
      Study s = requestManager.getStudyById(studyId);

      Long sequencerReferenceId = json.getLong("sequencerReferenceId");
      SequencerReference sr = requestManager.getSequencerReferenceById(sequencerReferenceId);

      StringBuilder sb = new StringBuilder();

      Experiment e = dataObjectFactory.getExperiment();
      e.setAlias("EXP_AUTOGEN_" + s.getName() + "_" + s.getStudyType() + "_" + (s.getExperiments().size() + 1));
      e.setTitle(s.getProject().getName() + " " + sr.getPlatform().getPlatformType().getKey() + " " + s.getStudyType() + " experiment (Auto-gen)");
      e.setDescription(s.getProject().getAlias());
      e.setPlatform(sr.getPlatform());
      e.setStudy(s);
      e.setSecurityProfile(s.getSecurityProfile());

      try {
        p.addExperiment(e);
        requestManager.saveExperiment(e);
      }
      catch (MalformedExperimentException e1) {
        e1.printStackTrace();
      }

      sb.append("<i>");
      sb.append("<span>" + s.getProject().getAlias() + " (" + e.getName() + ": " + p.getDilutions().size() + " dilutions)</span><br/>");
      sb.append("</i>");

      return JSONUtils.JSONObjectResponse("html", sb.toString());
    }
    catch (Exception e) {
      e.printStackTrace();
      return JSONUtils.SimpleJSONError("Failed " + e.getMessage());
    }
  }

  public JSONObject lookupContainer(HttpSession session, JSONObject json) {
    if (json.has("barcode") && !"".equals(json.getString("barcode"))) {
      try {
        String barcode = json.getString("barcode");
        Collection<SequencerPartitionContainer<SequencerPoolPartition>> fs = requestManager.listSequencerPartitionContainersByBarcode(barcode);
        if (!fs.isEmpty()) {
          JSONObject confirm = new JSONObject();
          StringBuilder sb = new StringBuilder();
          if (fs.size() == 1) {
            //replace container div
            SequencerPartitionContainer<SequencerPoolPartition> f = new ArrayList<SequencerPartitionContainer<SequencerPoolPartition>>(fs).get(0);
            sb.append("<table class='in'>");
            sb.append("<th>Partition No.</th>");
            sb.append("<th>Pool</th>");
            for (SequencerPoolPartition p : f.getPartitions()) {
              sb.append("<tr>");
              sb.append("<td>" + p.getPartitionNumber() + "</td>");
              sb.append("<td width='90%'>");
              if (p.getPool() != null) {
                confirm.put(p.getPartitionNumber(), p.getPool().getName());

                sb.append("<ul partition='" + (p.getPartitionNumber() - 1) + "' bind='partitions[" + (p.getPartitionNumber() - 1) + "].pool' class='runPartitionDroppable'>");
                sb.append("<div class='dashboard'>");
                sb.append(p.getPool().getName());
                sb.append("(" + p.getPool().getCreationDate() + ")<br/>");
                sb.append("<span style='font-size:8pt'>");
                if (!p.getPool().getExperiments().isEmpty()) {
                  sb.append("<i>");
                  for (Experiment e : p.getPool().getExperiments()) {
                    sb.append(e.getStudy().getProject().getAlias() + " (" + e.getName() + ": " + p.getPool().getDilutions().size() + " dilutions)<br/>");
                  }
                  sb.append("</i>");
                  sb.append("<input type='hidden' name='partitions[" + (p.getPartitionNumber() - 1) + "].pool' id='pId" + (p.getPartitionNumber() - 1) + "' value='" + p.getPool().getId() + "'/>");
                }
                else {
                  sb.append("<i>No experiment linked to this pool</i>");
                }
                sb.append("</span>");
                sb.append("</div>");
                sb.append("</ul>");
              }
              else {
                confirm.put(p.getPartitionNumber(), "Empty");

                sb.append("<div id='p_div_" + (p.getPartitionNumber() - 1) + "' class='elementListDroppableDiv'>");
                sb.append("<ul class='runPartitionDroppable' bind='partitions[" + (p.getPartitionNumber() - 1) + "].pool' partition='" + (p.getPartitionNumber() - 1) + "' ondblclick='Container.partition.populatePartition(this);'></ul>");
                sb.append("</div>");
              }
              sb.append("</td>");
              sb.append("</tr>");
            }
            sb.append("</table>");

            Map<String, Object> responseMap = new HashMap<String, Object>();
            responseMap.put("html", sb.toString());
            responseMap.put("barcode", f.getIdentificationBarcode());
            responseMap.put("verify", confirm);
            return JSONUtils.JSONObjectResponse(responseMap);
          }
          else {
            //choose container
            return JSONUtils.JSONObjectResponse("html", "");
          }
        }
        else {
          return JSONUtils.JSONObjectResponse("err", "No containers with this barcode.");
        }
      }
      catch (IOException e) {
        e.printStackTrace();
        return JSONUtils.JSONObjectResponse("err", "Unable to lookup barcode.");
      }
    }
    else {
      return JSONUtils.SimpleJSONError("Please supply a barcode to lookup.");
    }
  }

  public JSONObject listSequencePartitionContainersDataTable(HttpSession session, JSONObject json) {
    try {
      JSONObject j = new JSONObject();
      JSONArray jsonArray = new JSONArray();
      for (SequencerPartitionContainer<SequencerPoolPartition> sequencePartitionContainer : requestManager.listAllSequencerPartitionContainers()) {
        String run = "";
        String sequencer = "";
        if (sequencePartitionContainer.getRun() != null) {
          run = "<a href=\"/miso/run/" + sequencePartitionContainer.getRun().getId() + "\">" + sequencePartitionContainer.getRun().getAlias() + "</a>";
          if (sequencePartitionContainer.getRun().getSequencerReference() != null) {
            sequencer = "<a href=\"/miso/sequencer/" + sequencePartitionContainer.getRun().getSequencerReference().getId() + "\">"
                        + sequencePartitionContainer.getRun().getSequencerReference().getPlatform().getNameAndModel() + "</a>";
          }
        }
        jsonArray.add("['" +
                      (sequencePartitionContainer.getIdentificationBarcode() != null ? sequencePartitionContainer.getIdentificationBarcode() :"") +"','" +
                      (sequencePartitionContainer.getPlatformType()!=null? sequencePartitionContainer.getPlatformType().getKey() : "") + "','" +
                      run + "','" +
                      sequencer + "','" +
                      "<a href=\"/miso/container/" + sequencePartitionContainer.getId() + "\"><span class=\"ui-icon ui-icon-pencil\"></span></a>" + "']");

      }
      j.put("array", jsonArray);
      return j;
    }
    catch (IOException e) {
      log.debug("Failed", e);
      return JSONUtils.SimpleJSONError("Failed: " + e.getMessage());
    }
  }

  public void setSecurityManager(SecurityManager securityManager) {
    this.securityManager = securityManager;
  }

  public void setRequestManager(RequestManager requestManager) {
    this.requestManager = requestManager;
  }

  public void setDataObjectFactory(DataObjectFactory dataObjectFactory) {
    this.dataObjectFactory = dataObjectFactory;
  }
}