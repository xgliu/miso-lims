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
import net.sf.json.JSONObject;
import net.sourceforge.fluxion.ajax.Ajaxified;
import net.sourceforge.fluxion.ajax.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.ac.bbsrc.tgac.miso.core.data.Platform;
import uk.ac.bbsrc.tgac.miso.core.data.SequencerReference;
import uk.ac.bbsrc.tgac.miso.core.data.impl.SequencerReferenceImpl;
import uk.ac.bbsrc.tgac.miso.core.manager.RequestManager;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;

/**
 * uk.ac.bbsrc.tgac.miso.spring.ajax
 * <p/>
 * Info
 *
 * @author Rob Davey
 * @since 0.0.2
 */
@Ajaxified
public class SequencerReferenceControllerHelperService {
  protected static final Logger log = LoggerFactory.getLogger(SubmissionControllerHelperService.class);
  @Autowired
  private com.eaglegenomics.simlims.core.manager.SecurityManager securityManager;
  @Autowired
  private RequestManager requestManager;

  public JSONObject listPlatforms(HttpSession session, JSONObject json) {
    try {
      Collection<Platform> ps = requestManager.listAllPlatforms();
      StringBuilder sb = new StringBuilder();
      for (Platform p : ps) {
        sb.append("<option value="+p.getPlatformId()+">"+p.getNameAndModel()+"</option>");
      }
      return JSONUtils.JSONObjectResponse("platforms", sb.toString());
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return JSONUtils.SimpleJSONError("Cannot list available platforms");
  }

  public JSONObject listSequencers(HttpSession session, JSONObject json) {
    try {
      Collection<SequencerReference> sr = requestManager.listAllSequencerReferences();
      StringBuilder sb = new StringBuilder();
      for (SequencerReference s : sr) {
        sb.append("<option value="+s.getId()+">"+s.getPlatform().getNameAndModel()+" - "+s.getName()+"</option>");
      }
      return JSONUtils.JSONObjectResponse("sequencers", sb.toString());
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return JSONUtils.SimpleJSONError("Cannot list available sequencers");
  }

  public JSONObject checkServerAvailability(HttpSession session, JSONObject json) {
    try {

      if (json.has("server") && !json.get("server").equals("")) {
        InetAddress i = InetAddress.getByName(json.getString("server"));
        if (i.isReachable(2000)) {
          return JSONUtils.JSONObjectResponse("html", "OK");
        }
        else {
          return JSONUtils.JSONObjectResponse("html", "FAIL");
        }
      }
    }
    catch (Exception e) {
      log.debug("Failed to check server availability: ", e);
      return JSONUtils.JSONObjectResponse("html", "FAIL");
    }
    return JSONUtils.SimpleJSONError("Cannot check server availability");
  }

  public JSONObject addSequencerReference(HttpSession session, JSONObject json) {
    try {
      if (json.has("server") && !json.get("server").equals("")) {
        InetAddress i = InetAddress.getByName(json.getString("server"));
        String name = json.getString("name");
        Platform p = requestManager.getPlatformById(json.getInt("platform"));
        SequencerReference sr = new SequencerReferenceImpl(name, i, p);
        sr.setAvailable(i.isReachable(2000));
        log.info(sr.toString());
        requestManager.saveSequencerReference(sr);
        return JSONUtils.SimpleJSONResponse("Saved successfully");
      }
    }
    catch (Exception e) {
      log.debug("Failed to add server: ", e);
      return JSONUtils.SimpleJSONError("Failed to add server");
    }
    return JSONUtils.SimpleJSONError("Failed to add server");
  }

  public JSONObject deleteSequencerReference(HttpSession session, JSONObject json) {
    try {
      User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      if (user.isAdmin()) {
        if (json.has("refId")) {
          Long refId = json.getLong("refId");
          requestManager.deleteSequencerReference(requestManager.getSequencerReferenceById(refId));
          return JSONUtils.SimpleJSONResponse("Sequencer Reference deleted");
        }
        else {
          return JSONUtils.SimpleJSONError("No Sequencer Reference specified to delete.");
        }
      }
      else {
        return JSONUtils.SimpleJSONError("Only admins can delete objects.");
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      return JSONUtils.SimpleJSONError("Error getting currently logged in user.");
    }
  }

  public void setRequestManager(RequestManager requestManager) {
    this.requestManager = requestManager;
  }

  public void setSecurityManager(SecurityManager securityManager) {
    this.securityManager = securityManager;
  }
}
