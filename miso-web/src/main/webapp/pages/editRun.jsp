<%--
  ~ Copyright (c) 2012. The Genome Analysis Centre, Norwich, UK
  ~ MISO project contacts: Robert Davey, Mario Caccamo @ TGAC
  ~ **********************************************************************
  ~
  ~ This file is part of MISO.
  ~
  ~ MISO is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as published by
  ~ the Free Software Foundation, either version 3 of the License, or
  ~ (at your option) any later version.
  ~
  ~ MISO is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~ GNU General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with MISO.  If not, see <http://www.gnu.org/licenses/>.
  ~
  ~ **********************************************************************
  --%>

<%--
  Created by IntelliJ IDEA.
  User: davey
  Date: 15-Feb-2010
  Time: 15:09:13

--%>
<%@ include file="../header.jsp" %>

<script type="text/javascript" src="<c:url value='/scripts/run_ajax.js?ts=${timestamp.time}'/>"></script>
<script type="text/javascript" src="<c:url value='/scripts/run_validation.js?ts=${timestamp.time}'/>"></script>
<script type="text/javascript" src="<c:url value='/scripts/stats_ajax.js?ts=${timestamp.time}'/>"></script>

<c:choose>
    <c:when test="${not empty run.status}"><div id="maincontent" class="${run.status.health.key}"></c:when>
    <c:otherwise><div id="maincontent"></c:otherwise>
</c:choose>
<div id="contentcolumn">
<form:form method="POST" commandName="run" autocomplete="off" onsubmit="return validate_run(this);">
<h1>
    <c:choose>
        <c:when test="${not empty run.runId}">Edit</c:when>
        <c:otherwise>Create</c:otherwise>
    </c:choose> Run
    <button type="submit" class="fg-button ui-state-default ui-corner-all">Save</button>
</h1>
<div class="sectionDivider" onclick="toggleLeftInfo(jQuery('#note_arrowclick'), 'notediv');">Quick Help
    <div id="note_arrowclick" class="toggleLeft"></div>
</div>
<div id="notediv" class="note" style="display:none;">A Run contains the sequencing results from sequencing Experiments.
    Each run partition (lane/chamber) holds a Pool which is linked to a number of Experiments to facilitate multiplexing
    if required.
</div>
<h2>Run Information</h2>
<ul class="sddm" style="margin: 0px 8px 0 0;">
    <li><a
            onmouseover="mopen('runMenu')"
            onmouseout="mclosetime()">Options <span style="float:right"
                                                    class="ui-icon ui-icon-triangle-1-s"></span></a>

        <div id="runMenu"
             onmouseover="mcancelclosetime()"
             onmouseout="mclosetime()">
            <c:choose>
                <c:when test="${not empty runMap[run.runId]}">
                    <a href='javascript:void(0);' onclick="unwatchRun(${run.runId});">Stop watching</a>
                </c:when>
                <c:otherwise>
                    <a href='javascript:void(0);' onclick="watchRun(${run.runId});">Watch</a>
                </c:otherwise>
            </c:choose>
        </div>
    </li>
</ul>
<table class="in">
    <tr>
        <td class="h">Run ID:</td>
        <td>
            <c:choose>
                <c:when test="${not empty run.runId}"><input type='hidden' id='runId' name='runId'
                                                             value='${run.runId}'/>${run.runId}</c:when>
                <c:otherwise><i>Unsaved</i></c:otherwise>
            </c:choose>
        </td>
    </tr>
    <c:if test="${not empty run.accession}">
        <tr>
            <td class="h">Accession:</td>
            <td><a href="http://www.ebi.ac.uk/ena/data/view/${run.accession}" target="_blank">${run.accession}</a>
            </td>
                <%--<td><a href="void(0);" onclick="popup('help/runAccession.html');">Help</a></td>--%>
        </tr>
    </c:if>
    <tr>
        <c:choose>
            <c:when test="${empty run.runId}">
                <td>Platform:</td>
                <td>
                    <c:choose>
                        <c:when test="${not empty run.status and run.status.health.key ne 'Unknown'}"><form:radiobuttons
                                id="platformTypes" path="platformType" items="${platformTypes}"
                                onchange="changePlatformType(this, '${run.runId}');"
                                disabled="disabled"/></c:when>
                        <c:otherwise><form:radiobuttons id="platformTypes" path="platformType" items="${platformTypes}"
                                                        onchange="changePlatformType(this, '${run.runId}');"/></c:otherwise>

                    </c:choose>
                </td>
            </c:when>
            <c:otherwise>
                <td>Platform</td>
                <td>${run.platformType.key}</td>
            </c:otherwise>
        </c:choose>
    </tr>

    <tr>
        <c:choose>
            <c:when test="${empty run.runId}">
                <td>Sequencer:</td>
                <td id="sequencerReferenceSelect">
                    <i>Please choose a platform above...</i>
                </td>
            </c:when>
            <c:otherwise>
                <td>Sequencer</td>
                <td>${run.sequencerReference.name} - ${run.sequencerReference.platform.instrumentModel}</td>
            </c:otherwise>
        </c:choose>
    </tr>

    <tr>
        <td>Name:</td>
        <td>
            <c:choose>
                <c:when test="${not empty run.runId}">${run.name}</c:when>
                <c:otherwise><i>Unsaved</i></c:otherwise>
            </c:choose>
        </td>
            <%--<td><a href="void(0);" onclick="popup('help/runName.html');">Help</a></td>--%>
    </tr>
    <tr>
        <td class="h">Alias:</td>
        <td><form:input path="alias" class="validateable"/><span id="aliascounter" class="counter"></span><%--<a href="javascript:void(0);" onclick="retrieveRunInformation(jQuery('#alias').val());">Retrieve run information</a> --%>
        </td>
            <%--<td><a href="void(0);" onclick="popup('help/runAlias.html');">Help</a></td>--%>
    </tr>
    <tr>
        <td>Description:</td>
        <td>
            <c:choose>
                <c:when test="${not empty run.status and run.status.health.key ne 'Unknown'}"><form:input
                        path="description" disabled="disabled" class="validateable"/></c:when>
                <c:otherwise><form:input path="description" class="validateable"/></c:otherwise>
            </c:choose>
            <span id="descriptioncounter" class="counter"></span>
        </td>
            <%--<td><a href="void(0);" onclick="popup('help/runDescription.html');">Help</a></td>--%>
    </tr>
    <tr>
        <td>Run Path:</td>
        <td>
            <c:choose>
                <c:when test="${not empty run.filePath}">${run.filePath}</c:when>
                <c:otherwise><form:input path="filePath"/></c:otherwise>
            </c:choose>
        </td>
    </tr>
    <tr>
        <td>Paired End:</td>
        <td>
            <c:choose>
                <c:when test="${not empty run.status and run.status.health.key ne 'Unknown'}"><form:checkbox
                        value="${run.pairedEnd}" path="pairedEnd" disabled="disabled"/></c:when>
                <c:otherwise><form:checkbox value="${run.pairedEnd}" path="pairedEnd"/></c:otherwise>
            </c:choose>
        </td>
    </tr>

    <tr>
        <td valign="top">Status:</td>
        <td>
            <form:radiobuttons id="status.health" path="status.health" items="${healthTypes}" onchange="checkForCompletionDate();"/><br/>
            <table class="list" id="runStatusTable">
                <thead>
                <tr>
                    <th>Start Date</th>
                    <th>Completion Date</th>
                    <th>Last Updated</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td><fmt:formatDate pattern="dd/MM/yyyy" value="${run.status.startDate}"/></td>
                    <c:choose>
                        <c:when test="${(run.status.health.key eq 'Completed' and empty run.status.completionDate) or run.status.health.key eq 'Failed' or run.status.health.key eq 'Stopped'}">
                            <td><form:input path="status.completionDate"/></td>
                            <script type="text/javascript">
                                addDatePicker("status\\.completionDate");
                            </script>
                        </c:when>
                        <c:otherwise>
                            <td id="completionDate"><fmt:formatDate pattern="dd/MM/yyyy" value="${run.status.completionDate}"/></td>
                        </c:otherwise>
                    </c:choose>

                    <td><fmt:formatDate value="${run.status.lastUpdated}" dateStyle="long"
                                        pattern="dd/MM/yyyy HH:mm:ss"/></td>
                </tr>
                </tbody>
            </table>

            <c:if test="${not empty run.status.xml}">
                ${statusXml}
            </c:if>
        </td>
    </tr>

</table>
<%@ include file="permissions.jsp" %>
<c:if test="${not empty run.runId}">
    <c:if test="${statsAvailable}">
        <div class="sectionDivider" onclick="toggleLeftInfo(jQuery('#stats_arrowclick'), 'stats');">Statistics
            <div id="stats_arrowclick" class="toggleLeftDown"></div>
        </div>
        <div id="stats">
            <h1>Statistics</h1>

            <div id="summarydiv"></div>
        </div>
    </c:if>

    <div class="sectionDivider" onclick="toggleLeftInfo(jQuery('#notes_arrowclick'), 'notes');">Notes
        <div id="notes_arrowclick" class="toggleLeftDown"></div>
    </div>
    <div id="notes">
        <h1>Notes</h1>
        <ul class="sddm">
            <li><a
                    onmouseover="mopen('notesmenu')"
                    onmouseout="mclosetime()">Options <span style="float:right"
                                                            class="ui-icon ui-icon-triangle-1-s"></span></a>

                <div id="notesmenu"
                     onmouseover="mcancelclosetime()"
                     onmouseout="mclosetime()">
                    <a onclick="showRunNoteDialog(${run.runId});" href="javascript:void(0);" class="add">Add
                        Note</a>
                </div>
            </li>
        </ul>
        <c:if test="${fn:length(run.notes) > 0}">
            <div id="notelist" class="note" style="clear:both">
                <c:forEach items="${run.notes}" var="note" varStatus="n">
                    <div class="exppreview" id="run-notes-${n.count}">
                        <b>${note.creationDate}</b>: ${note.text}
          <span class="float-right"
                style="font-weight:bold; color:#C0C0C0;">${note.owner.loginName}</span>
                    </div>
                </c:forEach>
            </div>
        </c:if>
        <div id="addRunNoteDialog" title="Create new Note"></div>
    </div>
    <br/>
</c:if>

<c:if test="${not empty run.status and run.status.health.key eq 'Completed'}">
    <h1>Run QC</h1>
    <ul class="sddm">
        <li><a
                onmouseover="mopen('qcmenu')"
                onmouseout="mclosetime()">Options <span style="float:right"
                                                        class="ui-icon ui-icon-triangle-1-s"></span></a>

            <div id="qcmenu"
                 onmouseover="mcancelclosetime()"
                 onmouseout="mclosetime()">

                <a href='javascript:void(0);' class="add"
                   onclick="generateRunQCRow(${run.runId}); return false;">Add Run QC</a>

                <c:if test="${operationsQcPassed}">
                    <a href='<c:url value="/miso/analysis/new/run/${run.runId}"/>' class="add">Initiate Analysis</a>
                </c:if>
            </div>
        </li>
    </ul>
<span style="clear:both">
  <div id="addRunQC"></div>
  <table class="list in" id="runQcTable">
      <thead>
      <tr>
          <th>QCed By</th>
          <th>QC Date</th>
          <th>Method</th>
          <th>Process Selection</th>
          <th>Info</th>
          <th>Do Not Process</th>
      </tr>
      </thead>
      <tbody>
      <c:if test="${not empty run.runQCs}">
          <c:forEach items="${run.runQCs}" var="qc">
              <tr onMouseOver="this.className='highlightrow'" onMouseOut="this.className='normalrow'">
                  <td>${qc.qcCreator}</td>
                  <td><fmt:formatDate value="${qc.qcDate}"/></td>
                  <td>${qc.qcType.name}</td>
                  <td>
                      <c:forEach items="${run.sequencerPartitionContainers}" var="container" varStatus="fCount">
                          <table class="containerSummary">
                              <tr>
                                  <c:forEach items="${container.partitions}" var="partition">
                                      <c:if test="${not empty qc.partitionSelections and fn:length(qc.partitionSelections) > 0}">
                                          <c:forEach items="${qc.partitionSelections}" var="selection">
                                              <c:if test="${selection.partitionNumber eq partition.partitionNumber}">
                                                  <td id="${qc.qcId}_${run.runId}_${container.containerId}_${partition.partitionNumber}"
                                                      class="smallbox partitionOccupied">${partition.partitionNumber}</td>
                                              </c:if>
                                          </c:forEach>
                                      </c:if>
                                  </c:forEach>
                              </tr>
                          </table>
                          <c:if test="${fn:length(run.sequencerPartitionContainers) > 1 and fCount < fn:length(run.sequencerPartitionContainers)}">
                              <br/>
                          </c:if>
                      </c:forEach>
                  </td>
                  <td>${qc.information}</td>
                  <td>${qc.doNotProcess}</td>
              </tr>
          </c:forEach>
      </c:if>
      </tbody>
  </table>
</span>
</c:if>

<div id="runinfo">
<table width="100%">
<tbody>
<tr>
<td width="50%" valign="top">
    <h2>Run Parameters</h2>

    <div id="runPartitions">
        <c:choose>
            <c:when test="${empty run.sequencerPartitionContainers}">
                Container:
                <c:choose>
                    <c:when test="${not empty run.sequencerReference}">
                        <c:forEach var="platformContainerCount" begin="1"
                                   end="${run.sequencerReference.platform.numContainers}" step="1"
                                   varStatus="platformContainer">
                            <input id='container${platformContainerCount}select' name='containerselect'
                                   onchange="changeContainer(this.value, '${run.platformType.key}', ${run.sequencerReference.id});"
                                   type='radio'
                                   value='${platformContainerCount}'/>${platformContainerCount}
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <input id='container1select' name='containerselect'
                               onchange="changeContainer(this.value, '${run.platformType.key}', ${run.sequencerReference.id});"
                               type='radio' value='1'/>1
                    </c:otherwise>
                </c:choose>
                <br/>

                <div id='containerdiv' class="note ui-corner-all"></div>
            </c:when>
            <c:otherwise>
                <c:forEach items="${run.sequencerPartitionContainers}" var="container" varStatus="containerCount">
                    <div class="note ui-corner-all">
                        <h2>Container ${containerCount.count}</h2>
                        <c:if test="${not empty container.identificationBarcode}">
                            <ul class="sddm">
                                <li><a
                                        onmouseover="mopen('containermenu')"
                                        onmouseout="mclosetime()">Options <span style="float:right"
                                                                                class="ui-icon ui-icon-triangle-1-s"></span></a>

                                    <div class="run" id="containermenu"
                                         onmouseover="mcancelclosetime()"
                                         onmouseout="mclosetime()">
                                        <c:if test="${run.platformType.key eq 'Illumina'}">
                                            <a href="javascript:void(0);"
                                               onclick="generateCasava17DemultiplexCSV(${run.runId}, ${container.containerId});">Demultiplex
                                                CSV (pre-1.8)</a>
                                            <a href="javascript:void(0);"
                                               onclick="generateCasava18DemultiplexCSV(${run.runId}, ${container.containerId});">Demultiplex
                                                CSV (1.8+)</a>
                                        </c:if>
                                    </div>
                                </li>
                            </ul>
                        </c:if>
                        <div style="clear:both"></div>
                        <table class="in">
                            <tr>
                                <c:choose>
                                    <c:when test="${empty container.identificationBarcode}">
                                        <td>ID:</td>
                                        <td>
                                            <button onclick='lookupContainer(this, ${containerCount.index});'
                                                    type='button' class='right-button ui-state-default ui-corner-all'>
                                                Lookup
                                            </button>
                                            <div style='overflow:hidden'>
                                                <form:input
                                                        path="sequencerPartitionContainers[${containerCount.index}].identificationBarcode"/>
                                            </div>
                                        </td>
                                    </c:when>
                                    <c:otherwise>
                                        <td>ID:</td>
                                        <td>
                                            <span id="idBarcode">${container.identificationBarcode}</span>
                                            <c:if test="${(container.securityProfile.owner.loginName eq SPRING_SECURITY_CONTEXT.authentication.principal.username)
                                                    or fn:contains(SPRING_SECURITY_CONTEXT.authentication.principal.authorities,'ROLE_ADMIN')}">
                                                <a href="javascript:void(0);"
                                                   onclick="editContainerIdBarcode(jQuery('#idBarcode'), ${containerCount.index})">
                                                    <span class="fg-button ui-icon ui-icon-pencil"></span>
                                                </a>
                                            </c:if>
                                        </td>
                                    </c:otherwise>
                                </c:choose>
                            </tr>
                            <tr>
                                <c:choose>
                                    <c:when test="${empty container.locationBarcode}">
                                        <td>Location:</td>
                                        <td><form:input path="sequencerPartitionContainers[${containerCount.index}].locationBarcode"/></td>
                                    </c:when>
                                    <c:otherwise>
                                        <td>Location:</td>
                                        <td>
                                            <span id="locationBarcode">${container.locationBarcode}</span>
                                            <c:if test="${(container.securityProfile.owner.loginName eq SPRING_SECURITY_CONTEXT.authentication.principal.username)
                                                    or fn:contains(SPRING_SECURITY_CONTEXT.authentication.principal.authorities,'ROLE_ADMIN')}">
                                                <a href="javascript:void(0);"
                                                   onclick="editContainerLocationBarcode(jQuery('#locationBarcode'), ${containerCount.index})">
                                                    <span class="fg-button ui-icon ui-icon-pencil"></span>
                                                </a>
                                            </c:if>
                                        </td>
                                    </c:otherwise>
                                </c:choose>
                            </tr>
                            <tr>
                                <c:choose>
                                    <c:when test="${empty container.validationBarcode}">
                                        <td>Validation:</td>
                                        <td><form:input
                                                path="sequencerPartitionContainers[${containerCount.index}].validationBarcode"/></td>
                                    </c:when>
                                    <c:otherwise>
                                        <td>Validation:</td>
                                        <td>
                                            <span id="validationBarcode">${container.validationBarcode}</span>
                                            <c:if test="${(container.securityProfile.owner.loginName eq SPRING_SECURITY_CONTEXT.authentication.principal.username)
                                                    or fn:contains(SPRING_SECURITY_CONTEXT.authentication.principal.authorities,'ROLE_ADMIN')}">
                                                <a href="javascript:void(0);"
                                                   onclick="editContainerValidationBarcode(jQuery('#validationBarcode'), 0)">
                                                    <span class="fg-button ui-icon ui-icon-pencil"></span>
                                                </a>
                                            </c:if>
                                        </td>
                                    </c:otherwise>
                                </c:choose>
                            </tr>
                          <%--
                            <tr>
                                <td>Paired: ${container.paired}</td>
                            </tr>
                            --%>
                        </table>
                        <div id='partitionErrorDiv'></div>
                        <div id="partitionDiv">
                            <i class="italicInfo">Click in a partition box to beep/type in barcodes, or double click a
                                pool on the right to sequentially add pools to the container</i>
                            <table class="in">
                                <th>Partition No.</th>
                                <th>Pool</th>
                                <c:if test="${statsAvailable}">
                                    <th>Stats</th>
                                </c:if>
                                <c:forEach items="${container.partitions}" var="partition" varStatus="partitionCount">
                                    <tr>
                                        <td>${partition.partitionNumber}</td>
                                        <td width="90%">
                                            <c:choose>
                                                <c:when test="${not empty partition.pool}">
                                                    <div class="dashboard">
                                                      <a href='<c:url value="/miso/pool/${fn:toLowerCase(run.platformType.key)}/${partition.pool.poolId}"/>'>
                                                              ${partition.pool.name}
                                                          (${partition.pool.creationDate})
                                                      </a><br/>
                                                      <span style="font-size:8pt">
                                                        <c:choose>
                                                            <c:when test="${not empty partition.pool.experiments}">
                                                                <i><c:forEach items="${partition.pool.experiments}" var="experiment">
                                                                    ${experiment.study.project.alias} (${experiment.name}: ${fn:length(partition.pool.dilutions)} dilutions)<br/>
                                                                </c:forEach>
                                                                </i>
                                                                <input type="hidden"
                                                                       name="sequencerPartitionContainers[${containerCount.index}].partitions[${partitionCount.index}].pool"
                                                                       id="pId${partitionCount.index}"
                                                                       value="${partition.pool.poolId}"/>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <i>No experiment linked to this pool</i>
                                                            </c:otherwise>
                                                        </c:choose>
                                                      </span>
                                                    </div>
                                                </c:when>
                                                <c:otherwise>
                                                    <div id="p_div_${partitionCount.index}"
                                                         class="elementListDroppableDiv">
                                                        <div class="runPartitionDroppable"
                                                             bind="sequencerPartitionContainers[${containerCount.index}].partitions[${partitionCount.index}].pool"
                                                             partition="${containerCount.index}_${partitionCount.index}"
                                                             ondblclick='populatePartition(this, ${containerCount.index}, ${partitionCount.index});'></div>
                                                    </div>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <c:if test="${statsAvailable}">
                                            <td><img src="<c:url value='/styles/images/chart-bar-icon.png'/>" border="0"
                                                     onclick="Stats.getPartitionStats(${run.runId}, ${partition.partitionNumber});">
                                            </td>
                                        </c:if>
                                    </tr>
                                </c:forEach>
                            </table>
                        </div>
                        <input type="hidden" value="${container.containerId}"
                               id="sequencerPartitionContainers${containerCount.count-1}"
                               name="sequencerPartitionContainers"/>
                    </div>
                </c:forEach>
            </c:otherwise>
        </c:choose>
        <input type="hidden" value="on" name="_sequencerPartitionContainers"/>
    </div>
</td>
<td width="50%" valign="top">
    <h2>Available Pools</h2>
    <c:choose>
        <c:when test="${not empty run.platformType}">
            <input id="showOnlyReady" type="checkbox" checked="true"
                   onclick="toggleReadyToRunCheck(this, '${run.platformType.key}');"/>Only Ready to Run pools?
            <div align="right" style="margin-top: -23px; margin-bottom:3px">Filter: <input type="text"
                <%--onkeyup="poolSearch(this, true, '${run.platformType.key}');"--%>
                                                                                           size="8" id="searchPools"
                                                                                           name="searchPools"/></div>
            <script type="text/javascript">
                typewatchFunc(jQuery('#searchPools'), function() {
                    poolSearch(jQuery('#searchPools').val(), '${run.platformType.key}');
                }, 300, 2);
            </script>
        </c:when>
        <c:otherwise>
            <input id="showOnlyReady" type="checkbox" checked="true"
                   onclick="toggleReadyToRunCheck(this, jQuery('input[name=platformType]:checked').val());"/>Only Ready to Run pools?
            <div align="right" style="margin-top: -23px; margin-bottom:3px">Filter: <input type="text"
                <%--onkeyup="poolSearch(this, true, jQuery('input[name=platformType]:checked').val());"--%>
                                                                                           size="8" id="searchPools"
                                                                                           name="searchPools"/></div>
            <script type="text/javascript">
                typewatchFunc(jQuery('#searchPools'), function() {
                    poolSearch(jQuery('#searchPools').val(), jQuery('input[name=platformType]:checked').val());
                }, 300, 2);
            </script>
        </c:otherwise>
    </c:choose>
    <div id='poolList' class="elementList ui-corner-all" style="height:500px">
    </div>
</td>
</tr>
</tbody>
</table>
</div>

<script type="text/javascript">
    jQuery(document).ready(function() {
        jQuery('#alias').simplyCountable({
                                             counter: '#aliascounter',
                                             countType: 'characters',
                                             maxCount: ${maxLengths['alias']},
                                             countDirection: 'down'
                                         });

        jQuery('#description').simplyCountable({
                                                   counter: '#descriptioncounter',
                                                   countType: 'characters',
                                                   maxCount: ${maxLengths['description']},
                                                   countDirection: 'down'
                                               });

        <c:choose>
        <c:when test="${not empty run.platformType}">
        poolSearch("", '${run.platformType.key}');
        </c:when>
        <c:otherwise>
        poolSearch("", jQuery('input[name=platformType]:checked').val());
        </c:otherwise>
        </c:choose>

        <c:if test="${statsAvailable}">
        Stats.getRunStats(${run.runId});
        </c:if>
    });
</script>

<br/>
</form:form>
</div>
</div>


<%@ include file="adminsub.jsp" %>

<%@ include file="../footer.jsp" %>