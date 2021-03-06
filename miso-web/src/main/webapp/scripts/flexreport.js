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

var Reports = Reports || {
  generateProjectsFlexReport : function() {
    Utils.ui.disableButton('generateProjectsFlexReportButton');
    jQuery('#generateProjectsFlexReportFormHeader').html('<img src=\"/styles/images/ajax-loader.gif\"/>');
    Fluxion.doAjax(
            'flexReportingControllerHelperService',
            'generateProjectsFlexReport',
            {'form':jQuery('#generateProjectsFlexReportForm').serializeArray(), 'url':ajaxurl},
            {'doOnSuccess':function(json) {
              Utils.ui.reenableButton('generateProjectsFlexReportButton', 'Generate Report');

              jQuery('#generateProjectsFlexReportButton').fadeOut();
              jQuery('#generateProjectsFlexReportForm').fadeOut();

              if (jQuery('#projectProgress').val() == 'all' || json.graph.length > 1) {
                  Reports.chart.plotHighChartsPieChart(json.graph, "projectStatusChart", "Projects Status");
              }
              Reports.ui.createProjectOverviewTable(json.overviewTable);
              Reports.ui.createProjectResultTable(json.reportTable);
            }
            }
    );
  },
  generateProjectRunLaneFlexReport : function() {
    Utils.ui.disableButton('generateProjectRunLaneFlexReportButton');
    jQuery('#generateProjectRunLaneFlexReportFormHeader').html('<img src=\"/styles/images/ajax-loader.gif\"/>');
    Fluxion.doAjax(
            'flexReportingControllerHelperService',
            'generateProjectRunLaneFlexReport',
            {'form':jQuery('#generateProjectRunLaneFlexReportForm').serializeArray(), 'url':ajaxurl},
            {'doOnSuccess':function(json) {
              Utils.ui.reenableButton('generateProjectRunLaneFlexReportButton', 'Generate Report');

              jQuery('#generateProjectRunLaneFlexReportButton').fadeOut();
              jQuery('#generateProjectRunLaneFlexReportForm').fadeOut();

              Reports.ui.createProjectRunLaneResultTable(json.reportTable);
            }
            }
    );
  },

  generateSamplesFlexReport : function() {
    Utils.ui.disableButton('generateSamplesFlexReportButton');
    jQuery('#generateSamplesFlexReportFormHeader').html('<img src=\"/styles/images/ajax-loader.gif\"/>');
    Fluxion.doAjax(
            'flexReportingControllerHelperService',
            'generateSamplesFlexReport',
            {'form':jQuery('#generateSamplesFlexReportForm').serializeArray(), 'url':ajaxurl},
            {'doOnSuccess':function(json) {
              Utils.ui.reenableButton('generateSamplesFlexReportButton', 'Generate Report');

              jQuery('#generateSamplesFlexReportButton').fadeOut();
              jQuery('#generateSamplesFlexReportForm').fadeOut();

              if (jQuery('#sampleType').val() == 'all' || json.graph.length > 1) {
                  Reports.chart.plotHighChartsPieChart(json.graph, "sampleTypesChart", "Sample Types");
              }
              if (jQuery('#sampleQC').val() == 'all') {
                  Reports.chart.plotHighChartsPieChart(json.qcgraph, "sampleQcChart", "Sample QC");
              }
              Reports.ui.createSampleOverviewRelationTable(json.overviewRelationTable);
              Reports.ui.createSampleResultTable(json.reportTable);
            }
            }
    );
  },

  generateLibrariesFlexReport : function() {
    Utils.ui.disableButton('generateLibrariesFlexReportButton');
    jQuery('#generateLibrariesFlexReportFormHeader').html('<img src=\"/styles/images/ajax-loader.gif\"/>');
    Fluxion.doAjax(
            'flexReportingControllerHelperService',
            'generateLibrariesFlexReport',
            {'form':jQuery('#generateLibrariesFlexReportForm').serializeArray(), 'url':ajaxurl},
            {'doOnSuccess':function(json) {
              Utils.ui.reenableButton('generateLibrariesFlexReportButton', 'Generate Report');

              jQuery('#generateLibrariesFlexReportButton').fadeOut();
              jQuery('#generateLibrariesFlexReportForm').fadeOut();

              if (jQuery('#libraryPlatform').val() == 'all' || json.graph.length > 1) {
                  Reports.chart.plotHighChartsPieChart(json.graph, "libraryPlatformChart", "Platforms");
              }
              if (jQuery('#libraryQC').val() == 'all') {
                Reports.chart.plotHollowPieChart(json.qcgraph, "libraryQcChart");
                  Reports.chart.plotHighChartsPieChart(json.qcgraph, "libraryQcChart", "library QC");
              }
              Reports.ui.createLibraryOverviewRelationTable(json.overviewRelationTable);
              Reports.ui.createLibraryResultTable(json.reportTable);
            }
            }
    );
  },

  generateRunsFlexReport : function() {
    Utils.ui.disableButton('generateRunsFlexReportButton');
    jQuery('#generateRunsFlexReportFormHeader').html('<img src=\"/styles/images/ajax-loader.gif\"/>');
    Fluxion.doAjax(
            'flexReportingControllerHelperService',
            'generateRunsFlexReport',
            {'form':jQuery('#generateRunsFlexReportForm').serializeArray(), 'url':ajaxurl},
            {'doOnSuccess':function(json) {
              Utils.ui.reenableButton('generateRunsFlexReportButton', 'Generate Report');

              jQuery('#generateRunsFlexReportButton').fadeOut();
              jQuery('#generateRunsFlexReportForm').fadeOut();

              if (jQuery('#runStatus').val() == 'all' || json.graph.length > 1) {
                  Reports.chart.plotHighChartsPieChart(json.graph, "runStatusChart", "Runs Status");
              }
              if (jQuery('#runPlatform').val() == 'all' || json.platformgraph.length > 1) {
                  Reports.chart.plotHighChartsPieChart(json.platformgraph, "runPlatformChart", "Platform");
              }
              Reports.ui.createRunOverviewTable(json.overviewTable);
              Reports.ui.createRunResultTable(json.reportTable);
            }
            }
    );
  }
};

Reports.ui = {
  lastChecked : false,

  prepareTable : function() {
    var self = this;

    jQuery('.chkbox').click(function(event) {
      if (!self.lastChecked) {
        self.lastChecked = this;
        return;
      }

      if (event.shiftKey) {
        var start = jQuery('.chkbox').index(this);
        var end = jQuery('.chkbox').index(self.lastChecked);

        for (i = Math.min(start, end); i <= Math.max(start, end); i++) {
          jQuery('.chkbox')[i].checked = self.lastChecked.checked;
        }
      }

      self.lastChecked = this;
    });
    var theTable = jQuery("#table");

    jQuery("#filter").keyup(function() {
      jQuery.uiTableFilter(theTable, this.value);
    });

    jQuery('#filter-form').submit(
            function() {
              theTable.find("tbody > tr:visible > td:eq(1)").mousedown();
              return false;
            }).focus(); //Give focus to input field
  },

  initProjects : function() {
    var self = this;
    Utils.ui.disableButton('resetProjectsFlexReportButton');
    Fluxion.doAjax(
            'flexReportingControllerHelperService',
            'initProjects',
            {'url':ajaxurl},
            {"doOnSuccess":
                    function(json) {
                      jQuery('#generateProjectsFlexReportButton').fadeIn();
                      jQuery('#generateProjectsFlexReportForm').fadeIn();
                      jQuery('#projectStatusChart').html('');
                      jQuery('#projectOverviewCell').html('');
                      jQuery('#projectsFlexReport').html('');
                      self.createProjectFormTable(json.html);
                      jQuery('#projectProgress').html(json.progress);
                      Utils.ui.reenableButton('resetProjectsFlexReportButton', 'Reset');
                      self.prepareTable();
                    }
            }
    );
  },

  initSamples : function() {
    var self = this;
    Utils.ui.disableButton('resetSamplesFlexReportButton');
    Fluxion.doAjax(
            'flexReportingControllerHelperService',
            'initSamples',
            {'url':ajaxurl},
            {"doOnSuccess":
                    function(json) {
                      jQuery('#generateSamplesFlexReportButton').fadeIn();
                      jQuery('#generateSamplesFlexReportForm').fadeIn();
                      jQuery('#sampleStatusChart').html('');
                      jQuery('#sampleOverviewCell').html('');
                      jQuery('#samplesFlexReport').html('');
                      self.createSampleFormTable(json.html);
                      jQuery('#sampleType').html(json.type);
                      Utils.ui.reenableButton('resetSamplesFlexReportButton', 'Reset');
                      self.prepareTable();
                    }
            }
    );
  },

  initLibraries : function() {
    var self = this;
    Utils.ui.disableButton('resetLibrariesFlexReportButton');
    Fluxion.doAjax(
            'flexReportingControllerHelperService',
            'initLibraries',
            {'url':ajaxurl},
            {"doOnSuccess":
                    function(json) {
                      jQuery('#generateLibrariesFlexReportButton').fadeIn();
                      jQuery('#generateLibrariesFlexReportForm').fadeIn();
                      jQuery('#libraryStatusChart').html('');
                      jQuery('#libraryOverviewCell').html('');
                      jQuery('#librariesFlexReport').html('');
                      self.createLibraryFormTable(json.html);
                      jQuery('#libraryPlatform').html(json.platform);
                      Utils.ui.reenableButton('resetLibrariesFlexReportButton', 'Reset');
                      self.prepareTable();
                    }
            }
    );
  },

  initRuns : function() {
    var self = this;
    Utils.ui.disableButton('resetRunsFlexReportButton');
    Fluxion.doAjax(
            'flexReportingControllerHelperService',
            'initRuns',
            {'url':ajaxurl},
            {"doOnSuccess":
                    function(json) {
                      jQuery('#generateRunsFlexReportButton').fadeIn();
                      jQuery('#generateRunsFlexReportForm').fadeIn();
                      jQuery('#runStatusChart').html('');
                      jQuery('#runOverviewCell').html('');
                      jQuery('#runsFlexReport').html('');
                      self.createRunFormTable(json.html);
                      jQuery('#runPlatform').html(json.platform);
                      jQuery('#runStatus').html(json.status);
                      Utils.ui.reenableButton('resetRunsFlexReportButton', 'Reset');
                      self.prepareTable();
                    }
            }
    );
  },

  toggleCheckedItems : function(className, status) {
    jQuery("." + className).each(function() {
      jQuery(this).attr("checked", status);
    })
  },

  createProjectFormTable : function(array) {
    jQuery('#projectsResultTable').html('<table cellpadding="0" cellspacing="0" border="0" class="display" id="projectsResultFormTable"></table>');
    jQuery('#projectsResultFormTable').dataTable({
                                                   "aaData": array,
                                                   "aoColumns": [
                                                     {"sTitle":"<input class=\"chkbox1\" type=\"checkbox\" onclick=\"Reports.ui.toggleCheckedItems('chkboxprojects',this.checked)\"/> All"},
                                                     { "sTitle": "Project Name"},
                                                     { "sTitle": "Alias"},
                                                     { "sTitle": "Description"},
                                                     { "sTitle": "Progress"}
                                                   ],
                                                   "bPaginate": false,
                                                   "bFilter": false,
                                                   "bSort": false,
                                                   "sDom":'<"top"i>',
                                                   "bAutoWidth": false
                                                 });
  },

  createProjectRunLaneFormTable : function(array) {
    jQuery('#projectRunLaneResultTable').html('<table cellpadding="0" cellspacing="0" border="0" class="display" id="projectRunLaneResultFormTable"></table>');
    jQuery('#projectRunLaneResultFormTable').dataTable({
                                                   "aaData": array,
                                                   "aoColumns": [
                                                   //  {"sTitle":"<input class=\"chkbox1\" type=\"checkbox\" onclick=\"Reports.ui.toggleCheckedItems('chkboxprojectrunlane',this.checked)\"/> All"},
                                                     { "sTitle": "Project Name"},
                                                     { "sTitle": "Alias"},
                                                     { "sTitle": "Description"},
                                                     { "sTitle": "Run & Lane"}
                                                   ],
                                                   "bPaginate": false,
                                                   "bFilter": false,
                                                   "bSort": false,
                                                   "sDom":'<"top"i>',
                                                   "bAutoWidth": false
                                                 });
  },

  createSampleFormTable : function(array) {
    jQuery('#samplesResultTable').html('<table cellpadding="0" cellspacing="0" border="0" class="display" id="samplesResultFormTable"></table>');
    jQuery('#samplesResultFormTable').dataTable({
                                                  "aaData": array,
                                                  "aoColumns": [
                                                    {"sTitle":"<input class=\"chkbox1\" type=\"checkbox\" onclick=\"Reports.ui.toggleCheckedItems('chkboxsamples',this.checked)\"/> All"},
                                                    { "sTitle": "Sample Name"},
                                                    { "sTitle": "Alias"},
                                                    { "sTitle": "Description"},
                                                    { "sTitle": "Sample Type"},
                                                    { "sTitle": "QC Passed"}
                                                  ],
                                                  "bPaginate": false,
                                                  "bFilter": false,
                                                  "bSort": false,
                                                  "sDom":'<"top"i>',
                                                  "bAutoWidth": false
                                                });
  },

  createLibraryFormTable : function(array) {
    jQuery('#librariesResultTable').html('<table cellpadding="0" cellspacing="0" border="0" class="display" id="librariesResultFormTable"></table>');
    jQuery('#librariesResultFormTable').dataTable({
                                                    "aaData": array,
                                                    "aoColumns": [
                                                      {"sTitle":"<input class=\"chkbox1\" type=\"checkbox\" onclick=\"Reports.ui.toggleCheckedItems('chkboxlibraries',this.checked)\"/> All"},
                                                      { "sTitle": "Library Name"},
                                                      { "sTitle": "Alias"},
                                                      { "sTitle": "Description"},
                                                      { "sTitle": "Platform"},
                                                      { "sTitle": "Library Type"},
                                                      { "sTitle": "QC Passed"}
                                                    ],
                                                    "bPaginate": false,
                                                    "bFilter": false,
                                                    "bSort": false,
                                                    "sDom":'<"top"i>',
                                                    "bAutoWidth": false
                                                  });
  },

  createRunFormTable : function(array) {
    jQuery('#runsResultTable').html('<table cellpadding="0" cellspacing="0" border="0" class="display" id="runsResultFormTable"></table>');
    jQuery('#runsResultFormTable').dataTable({
                                               "aaData": array,
                                               "aoColumns": [
                                                 {"sTitle":"<input class=\"chkbox1\" type=\"checkbox\" onclick=\"Reports.ui.toggleCheckedItems('chkboxruns',this.checked)\"/> All"},
                                                 { "sTitle": "Run Name"},
                                                 { "sTitle": "Alias"},
                                                 { "sTitle": "Status"},
                                                 { "sTitle": "Platform"}
                                               ],
                                               "bPaginate": false,
                                               "bFilter": false,
                                               "bSort": false,
                                               "sDom":'<"top"i>',
                                               "bAutoWidth": false
                                             });
  },

  createProjectOverviewTable : function(array) {
    jQuery('#projectOverviewCell').html('<table cellpadding="0" cellspacing="0" border="0" class="display" id="projectOverviewTable"></table>');
    jQuery('#projectOverviewTable').dataTable({
                                                "aaData": array,
                                                "aoColumns": [
                                                  { "sTitle": "Status"},
                                                  { "sTitle": "Amount"}
                                                ],
                                                "bJQueryUI": true
                                              });
  },

  createProjectResultTable : function(array) {
    jQuery('#projectsFlexReport').html('<table cellpadding="0" cellspacing="0" border="0" class="display" id="projectsFlexReportTable"></table>');
    jQuery.fn.dataTableExt.oSort['no-pro-asc'] = function(x, y) {
      var a = parseInt(x.replace(/^PRO/i, ""));
      var b = parseInt(y.replace(/^PRO/i, ""));
      return ((a < b) ? -1 : ((a > b) ? 1 : 0));
    };
    jQuery.fn.dataTableExt.oSort['no-pro-desc'] = function(x, y) {
      var a = parseInt(x.replace(/^PRO/i, ""));
      var b = parseInt(y.replace(/^PRO/i, ""));
      return ((a < b) ? 1 : ((a > b) ? -1 : 0));
    };
    jQuery('#projectsFlexReportTable').dataTable({
                                                   "aaData": array,
                                                   "aoColumns": [
                                                     { "sTitle": "Project Name", "sType":"no-pro"},
                                                     { "sTitle": "Alias"},
                                                     { "sTitle": "Description"},
                                                     { "sTitle": "Progress"}
                                                   ],
                                                   "aaSorting":[
                                                     [0,"desc"]
                                                   ],
                                                   "bJQueryUI": true
                                                 });
  },

  createProjectRunLaneResultTable : function(array) {
    jQuery('#projectRunLaneFlexReport').html('<table cellpadding="0" cellspacing="0" border="0" class="display" id="projectRunLaneFlexReportTable"></table>');
    jQuery.fn.dataTableExt.oSort['no-pro-asc'] = function(x, y) {
      var a = parseInt(x.replace(/^PRO/i, ""));
      var b = parseInt(y.replace(/^PRO/i, ""));
      return ((a < b) ? -1 : ((a > b) ? 1 : 0));
    };
    jQuery.fn.dataTableExt.oSort['no-pro-desc'] = function(x, y) {
      var a = parseInt(x.replace(/^PRO/i, ""));
      var b = parseInt(y.replace(/^PRO/i, ""));
      return ((a < b) ? 1 : ((a > b) ? -1 : 0));
    };
    jQuery('#projectRunLaneFlexReportTable').dataTable({
                                                   "aaData": array,
                                                   "aoColumns": [
                                                     { "sTitle": "Project Name", "sType":"no-pro"},
                                                     { "sTitle": "Alias"},
                                                     { "sTitle": "Description"},
                                                     { "sTitle": "Run & Lane"}
                                                   ],
                                                   "aaSorting":[
                                                     [0,"desc"]
                                                   ],
                                                   "bJQueryUI": true
                                                 });
  },

  createSampleOverviewRelationTable : function(array) {
    jQuery('#sampleOverviewCell').append('<table cellpadding="0" cellspacing="0" border="0" class="display" id="sampleOverviewRelationTable"></table>');
    jQuery('#sampleOverviewRelationTable').dataTable({
                                                       "aaData": array,
                                                       "aoColumns": [
                                                         { "sTitle": "Sample Type"},
                                                         { "sTitle": "Created"},
                                                         { "sTitle": "Received"},
                                                         { "sTitle": "QC Passed"},
                                                         { "sTitle": "QC Failed"}
                                                       ],
                                                       "aaSorting":[
                                                         [0,"desc"]
                                                       ],
                                                       "bJQueryUI": true,
                                                       "bSort": false
                                                     });
  },

  createSampleOverviewTable : function(array) {
    jQuery('#sampleOverviewCell').append('<br/><br/><table cellpadding="0" cellspacing="0" border="0" class="display" id="sampleOverviewTable"></table>');
    jQuery('#sampleOverviewTable').dataTable({
                                               "aaData": array,
                                               "aoColumns": [
                                                 { "sTitle": "Status"},
                                                 { "sTitle": "Amount"}
                                               ],
                                               "bJQueryUI": true
                                             });
  },

  createSampleResultTable : function(array) {
    jQuery('#samplesFlexReport').html('<table cellpadding="0" cellspacing="0" border="0" class="display" id="samplesFlexReportTable"></table>');
    jQuery.fn.dataTableExt.oSort['no-sam-asc'] = function(x, y) {
      var a = parseInt(x.replace(/^SAM/i, ""));
      var b = parseInt(y.replace(/^SAM/i, ""));
      return ((a < b) ? -1 : ((a > b) ? 1 : 0));
    };
    jQuery.fn.dataTableExt.oSort['no-sam-desc'] = function(x, y) {
      var a = parseInt(x.replace(/^SAM/i, ""));
      var b = parseInt(y.replace(/^SAM/i, ""));
      return ((a < b) ? 1 : ((a > b) ? -1 : 0));
    };
    jQuery('#samplesFlexReportTable').dataTable({
                                                  "aaData": array,
                                                  "aoColumns": [
                                                    { "sTitle": "Sample Name", "sType":"no-sam"},
                                                    { "sTitle": "Alias"},
                                                    { "sTitle": "Description"},
                                                    { "sTitle": "Type"},
                                                    { "sTitle": "QC Passed"}
                                                  ],
                                                  "aaSorting":[
                                                    [0,"desc"]
                                                  ],
                                                  "bJQueryUI": true
                                                });
  },

  createLibraryOverviewRelationTable : function(array) {
    jQuery('#libraryOverviewCell').append('<table cellpadding="0" cellspacing="0" border="0" class="display" id="libraryOverviewRelationTable"></table>');
    jQuery('#libraryOverviewRelationTable').dataTable({
                                                        "aaData": array,
                                                        "aoColumns": [
                                                          { "sTitle": "Library Type"},
                                                          { "sTitle": "Platform"},
                                                          { "sTitle": "QC Passed"},
                                                          { "sTitle": "QC Failed"},
                                                          { "sTitle": "Total"}
                                                        ],
                                                        "bJQueryUI": true,
                                                        "bSort": false
                                                      });
  },

  createLibraryOverviewTable : function(array) {
    jQuery('#libraryOverviewCell').append('<br/><br/><table cellpadding="0" cellspacing="0" border="0" class="display" id="libraryOverviewTable"></table>');
    jQuery('#libraryOverviewTable').dataTable({
                                                "aaData": array,
                                                "aoColumns": [
                                                  { "sTitle": "Status"},
                                                  { "sTitle": "Amount"}
                                                ],
                                                "bJQueryUI": true
                                              });
  },

  createLibraryResultTable : function(array) {
    jQuery('#librariesFlexReport').html('<table cellpadding="0" cellspacing="0" border="0" class="display" id="librariesFlexReportTable"></table>');
    jQuery.fn.dataTableExt.oSort['no-lib-asc'] = function(x, y) {
      var a = parseInt(x.replace(/^LIB/i, ""));
      var b = parseInt(y.replace(/^LIB/i, ""));
      return ((a < b) ? -1 : ((a > b) ? 1 : 0));
    };
    jQuery.fn.dataTableExt.oSort['no-lib-desc'] = function(x, y) {
      var a = parseInt(x.replace(/^LIB/i, ""));
      var b = parseInt(y.replace(/^LIB/i, ""));
      return ((a < b) ? 1 : ((a > b) ? -1 : 0));
    };
    jQuery('#librariesFlexReportTable').dataTable({
                                                    "aaData": array,
                                                    "aoColumns": [
                                                      { "sTitle": "Library Name", "sType":"no-lib"},
                                                      { "sTitle": "Alias"},
                                                      { "sTitle": "Description"},
                                                      { "sTitle": "Platform"},
                                                      { "sTitle": "Library Type"},
                                                      { "sTitle": "QC Passed"}
                                                    ],
                                                    "aaSorting":[
                                                      [0,"desc"]
                                                    ],
                                                    "bJQueryUI": true
                                                  });
  },

  createRunOverviewTable : function(array) {
    jQuery('#runOverviewCell').html('<table cellpadding="0" cellspacing="0" border="0" class="display" id="runOverviewTable"></table>');
    jQuery('#runOverviewTable').dataTable({
                                            "aaData": array,
                                            "aoColumns": [
                                              { "sTitle": "Status"},
                                              { "sTitle": "Amount"}
                                            ],
                                            "bJQueryUI": true
                                          });
  },

  createRunResultTable : function(array) {
    jQuery('#runsFlexReport').html('<table cellpadding="0" cellspacing="0" border="0" class="display" id="runsFlexReportTable"></table>');
    jQuery.fn.dataTableExt.oSort['no-run-asc'] = function(x, y) {
      var a = parseInt(x.replace(/^RUN/i, ""));
      var b = parseInt(y.replace(/^RUN/i, ""));
      return ((a < b) ? -1 : ((a > b) ? 1 : 0));
    };
    jQuery.fn.dataTableExt.oSort['no-run-desc'] = function(x, y) {
      var a = parseInt(x.replace(/^RUN/i, ""));
      var b = parseInt(y.replace(/^RUN/i, ""));
      return ((a < b) ? 1 : ((a > b) ? -1 : 0));
    };
    jQuery('#runsFlexReportTable').dataTable({
                                               "aaData": array,
                                               "aoColumns": [
                                                 { "sTitle": "Run Name", "sType":"no-run"},
                                                 { "sTitle": "Alias"},
                                                 { "sTitle": "Status"},
                                                 { "sTitle": "Type"}
                                               ],
                                               "aaSorting":[
                                                 [0,"desc"]
                                               ],
                                               "bJQueryUI": true
                                             });
  },

  getSequencersList : function() {
    var self = this;
    Fluxion.doAjax(
            'sequencerReferenceControllerHelperService',
            'listSequencers',
            {'url':ajaxurl},
            {'doOnSuccess':function(json) {
              var list = "<select id='sequencers' name='sequencers' onchange='Reports.ui.updateCalendar();'> <option value=0> All </option>" + json.sequencers + "</select>";
              jQuery('#sequencerslist').html("<b> Sequencer Machines: </b>" + list);
              self.generateColors();

              init(jQuery('#datepicking').val(), jQuery('#sequencers').val(), jQuery('#sequencers option:last').val());
              addRestrictedDatePicker("calendarfrom");
              addRestrictedDatePicker("calendarto");

              self.updateCalendar();
            }
            }
    );
  },

  updateCalendar : function() {
    if (jQuery('#sequencers').val() == 0) {
      jQuery("#listtoggle").fadeIn();
    }
    else {
      jQuery("#listtoggle").fadeOut();
    }

    if (jQuery('#datepicking').val() == "custom") {
      jQuery("#calendarfrom").val("");
      jQuery("#calendarto").val("");
      jQuery("#custom").show();
    }
    else {
      jQuery("#custom").hide();
      drawCalendar(jQuery('#datepicking').val(), jQuery('#sequencers').val(), jQuery('#sequencers option:last').val());
    }
  },

  resetCalendar : function() {
    jQuery("#calendarfrom").val("");
    jQuery("#calendarto").val("");
    drawCalendar("cyear", jQuery('#sequencers').val(), jQuery('#sequencers option:last').val());
  },

  generateColors : function() {
    var size = jQuery('#sequencers option:last').val();
    var color = "<table>";
    var tempcolour = d3.scale.category20();

    for (var i = 1; i < size; i++) {
      if ((i % 2) > 0) {
        color += "<tr>";
      }
      var label = jQuery("#sequencers option").eq(i).text();
      var temp = parseInt(i) * parseInt(i);
      color += "<td bgcolor=" + tempcolour(temp) + "> <font color='black'> <b> " + label + "</b> </font></td>";
      if ((i % 2) == 0) {
        color += "</tr>";
      }
    }
    color += "</table>";
    jQuery("#legendlist").html(color);
  }
};

Reports.chart = {
  plotPieChart : function(datachart, div) {
    console.log(datachart.toString());
    var w = 300, h = 300, r = 100, color = d3.scale.category20c();
    var vis = d3.select("#" + div)
            .append("svg:svg")
            .data([datachart])
            .attr("width", w)
            .attr("height", h)
            .append("svg:g")
            .attr("transform", "translate(" + r + "," + r + ")");

    var arc = d3.svg.arc()
            .outerRadius(r);

    var pie = d3.layout.pie()
            .value(function(d) {
                     return d.value;
                   });
    var arcs = vis.selectAll("g.slice")
            .data(pie)
            .enter()
            .append("svg:g")
            .attr("class", "slice");

    arcs.append("svg:path")
            .attr("fill", function(d, i) {
                    return color(i);
                  })
            .attr("d", arc)
            .append("svg:title")
            .text(function(d, i) {
                    return datachart[i].label;
                  });

    arcs.append("svg:text")
            .attr("transform", function(d) {
                    d.innerRadius = 0;
                    d.outerRadius = r;
                    return "translate(" + arc.centroid(d) + ")";
                  })
            .attr("text-anchor", "middle")
            .text(function(d, i) {
                    return datachart[i].label;
                  });
  },

  plotHollowPieChart : function(data, div) {
    var w = 400, h = 600, r = 150, labelr = r + 10, sum = 0 // radius for label anchor
    color = d3.scale.category20c(), donut = d3.layout.pie(), arc = d3.svg.arc().innerRadius(r * .6).outerRadius(r);

    var vis = d3.select("#" + div)
            .append("svg:svg")
            .data([data])
            .attr("width", w + 150)
            .attr("height", h);

    var arcs = vis.selectAll("g.arc")
            .data(donut.value(function(d) {
      sum += parseInt(d.value);
      return d.value
    }))
            .enter().append("svg:g")
            .attr("class", "arc")
            .attr("transform", "translate(" + (r + 120) + "," + (r + 140) + ")");

    arcs.append("svg:path")
            .attr("fill", function(d, i) {
                    return color(i);
                  })
            .attr("d", arc)
            .append("svg:title")
            .text(function(d, i) {
                    return data[i].label;
                  });

    arcs.append("svg:text")
            .attr("transform", function(d, i) {
                    var c = arc.centroid(d), x = c[0], y = c[1], h = Math.sqrt(x * x + y * y);
                    return ((data[i].value * 360 / sum) < 10) ? "rotate (" + (c[0] - 90) + ")translate(" + (labelr) + "," + ((data[i].value * 360 / sum) * 2) + ")" : "translate(" + (x / h * labelr) + ',' + (y / h * labelr) + ")";
                    //                  return "translate(" + (x / h * labelr) + ',' +
                    //                         (y / h * labelr) + ")";
                  })
            .attr("dy", ".35em")
            .attr("text-anchor", "middle")
            .text(function(d, i) {
                    return data[i].label;
                  });
  },
  plotHighChartsPieChart : function(data, div, title){
    var chart;
    chart = new Highcharts.Chart({
        chart: {
            renderTo: div,
            plotBackgroundColor: null,
            plotBorderWidth: null,
            plotShadow: false
        },
        title: {
            text: title
        },
        tooltip: {
    	    pointFormat: '<b>{point.y}</b>'
        },
        plotOptions: {
            pie: {
                allowPointSelect: true,
                cursor: 'pointer',
                dataLabels: {
                    enabled: true,
                    color: '#000000',
                    connectorColor: '#000000',
                    formatter: function() {
                        return '<b>'+ this.point.name +'</b>: '+ this.point.y ;
                    }
                }
            }
        },
        series: [{
            type: 'pie',
            name: 'Amount',
            data: data
        }]
    });
  }
};

Reports.search = {
  searchProjects : function() {
    Utils.ui.disableButton('searchProjects');
    jQuery('#projectsResultTable').html('<img src=\"/styles/images/ajax-loader.gif\"/>');
    Fluxion.doAjax(
            'flexReportingControllerHelperService',
            'searchProjectsByCreationDateandString',
            {'str':jQuery('#projectReportSearchInput').val(),
              'from':jQuery('#from').val(),'to':jQuery('#to').val(),
              'progress':jQuery('#projectProgress option:selected').val(), 'url':ajaxurl},
            {"doOnSuccess":
                    function(json) {
                      Reports.ui.createProjectFormTable(json.html);
                      Utils.ui.reenableButton('searchProjects', 'Search');
                      Reports.ui.prepareTable();
                    }
            }
    );
  },
  searchProjectRunLane : function() {
    Utils.ui.disableButton('searchProjectRunLane');
    jQuery('#projectRunLaneResultTable').html('<img src=\"/styles/images/ajax-loader.gif\"/>');
    Fluxion.doAjax(
            'flexReportingControllerHelperService',
            'searchProjectsByRunCompletionDateandString',
            {'str':jQuery('#projectRunLaneReportSearchInput').val(),
              'from':jQuery('#projectRunLanefrom').val(),'to':jQuery('#projectRunLaneto').val(),
              'url':ajaxurl},
            {"doOnSuccess":
                    function(json) {
                      Reports.ui.createProjectRunLaneFormTable(json.html);
                      Utils.ui.reenableButton('searchProjectRunLane', 'Search');
                      Reports.ui.prepareTable();
                    }
            }
    );
  },

  searchSamples : function() {
    Utils.ui.disableButton('searchSamples');
    jQuery('#samplesResultTable').html('<img src=\"/styles/images/ajax-loader.gif\"/>');
    Fluxion.doAjax(
            'flexReportingControllerHelperService',
            'searchSamplesByCreationDateandString',
            {'str':jQuery('#sampleReportSearchInput').val(),
              'from':jQuery('#samplefrom').val(),'to':jQuery('#sampleto').val(),
              'type':jQuery('#sampleType option:selected').val(),
              'qc':jQuery('#sampleQC option:selected').val(),
              'url':ajaxurl},
            {"doOnSuccess":
                    function(json) {
                      Reports.ui.createSampleFormTable(json.html);
                      Utils.ui.reenableButton('searchSamples', 'Search');
                      Reports.ui.prepareTable();
                    }
            }
    );
  },

  searchLibraries : function() {
    Utils.ui.disableButton('searchLibraries');
    jQuery('#librariesResultTable').html('<img src=\"/styles/images/ajax-loader.gif\"/>');
    Fluxion.doAjax(
            'flexReportingControllerHelperService',
            'searchLibrariesByCreationDateandString',
            {'str':jQuery('#libraryReportSearchInput').val(),
              'from':jQuery('#libraryfrom').val(),'to':jQuery('#libraryto').val(),
              'platform':jQuery('#libraryPlatform option:selected').val(),
              'qc':jQuery('#libraryQC option:selected').val(),
              'url':ajaxurl},
            {"doOnSuccess":
                    function(json) {
                      Reports.ui.createLibraryFormTable(json.html);
                      Utils.ui.reenableButton('searchLibraries', 'Search');
                      Reports.ui.prepareTable();
                    }
            }
    );
  },

  searchRuns : function() {
    Utils.ui.disableButton('searchRuns');
    jQuery('#runsResultTable').html('<img src=\"/styles/images/ajax-loader.gif\"/>');
    Fluxion.doAjax(
            'flexReportingControllerHelperService',
            'searchRunsByCreationDateandString',
            {'str':jQuery('#runReportSearchInput').val(),
              'from':jQuery('#runfrom').val(),'to':jQuery('#runto').val(),
              'platform':jQuery('#runPlatform option:selected').val(),
              'status':jQuery('#runStatus option:selected').val(),
              'url':ajaxurl},
            {"doOnSuccess":
                    function(json) {
                      Reports.ui.createRunFormTable(json.html);
                      Utils.ui.reenableButton('searchRuns', 'Search');
                      Reports.ui.prepareTable();
                    }
            }
    );
  }
};

jQuery(function() {
  var dates = jQuery("#calendarfrom, #calendarto").datepicker({
                                                                defaultDate: "+1w",
                                                                changeMonth: true,
                                                                changeYear: true,
                                                                numberOfMonths: 1,
                                                                dateFormat: 'dd-mm-yy',
                                                                onSelect: function(selectedDate) {
                                                                  var option = this.id == "calendarfrom" ? "minDate" : "maxDate", instance = jQuery(this).data("datepicker"), date = jQuery.datepicker.parseDate(
                                                                          instance.settings.dateFormat ||
                                                                          jQuery.datepicker._defaults.dateFormat,
                                                                          selectedDate, instance.settings
                                                                  );
                                                                  dates.not(this).datepicker("option", option, date);
                                                                }
                                                              });
});