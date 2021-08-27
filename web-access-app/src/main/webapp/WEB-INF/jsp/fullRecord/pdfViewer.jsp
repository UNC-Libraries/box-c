<%--

    Copyright 2008 The University of North Carolina at Chapel Hill

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<link rel="stylesheet" href="/static/plugins/pdfjs/web/viewer.css">
<link rel="resource" type="application/l10n" href="/static/plugins/pdfjs/web/locale/locale.properties">
<script src="/static/plugins/pdfjs/build/pdf.js" type="text/javascript"></script>

<div id="pdfViewer">
    <div id="outerContainer" data-url='${briefObject.id}'>
        <div id="sidebarContainer">
            <div id="toolbarSidebar">
                <div class="splitToolbarButton toggled">
                    <button id="viewThumbnail" class="toolbarButton toggled" title="Show Thumbnails">
                        <span data-l10n-id="thumbs_label">Thumbnails</span>
                    </button>
                    <button id="viewOutline" class="toolbarButton" title="Show Document Outline (double-click to expand/collapse all items)">
                        <span data-l10n-id="document_outline_label">Document Outline</span>
                    </button>
                    <button id="viewAttachments" class="toolbarButton" title="Show Attachments">
                        <span data-l10n-id="attachments_label">Attachments</span>
                    </button>
                    <button id="viewLayers" class="toolbarButton" title="Show Layers (double-click to reset all layers to the default state)">
                        <span data-l10n-id="layers_label">Layers</span>
                    </button>
                </div>
            </div>
            <div id="sidebarContent">
                <div id="thumbnailView">
                </div>
                <div id="outlineView" class="hidden">
                </div>
                <div id="attachmentsView" class="hidden">
                </div>
                <div id="layersView" class="hidden">
                </div>
            </div>
            <div id="sidebarResizer" class="hidden"></div>
        </div>  <!-- sidebarContainer -->

        <div id="mainContainer">
            <div class="findbar hidden doorHanger" id="findbar">
                <div id="findbarInputContainer">
                    <input id="findInput" class="toolbarField" title="Find" placeholder="Find in document…">
                    <div class="splitToolbarButton">
                        <button id="findPrevious" class="toolbarButton findPrevious" title="Find the previous occurrence of the phrase">
                            <span data-l10n-id="find_previous_label">Previous</span>
                        </button>
                        <div class="splitToolbarButtonSeparator"></div>
                        <button id="findNext" class="toolbarButton findNext" title="Find the next occurrence of the phrase">
                            <span data-l10n-id="find_next_label">Next</span>
                        </button>
                    </div>
                </div>

                <div id="findbarOptionsOneContainer">
                    <input type="checkbox" id="findHighlightAll" class="toolbarField">
                    <label for="findHighlightAll" class="toolbarLabel" data-l10n-id="find_highlight">Highlight all</label>
                    <input type="checkbox" id="findMatchCase" class="toolbarField">
                    <label for="findMatchCase" class="toolbarLabel" data-l10n-id="find_match_case_label">Match case</label>
                </div>
                <div id="findbarOptionsTwoContainer">
                    <input type="checkbox" id="findEntireWord" class="toolbarField">
                    <label for="findEntireWord" class="toolbarLabel" data-l10n-id="find_entire_word_label">Whole words</label>
                    <span id="findResultsCount" class="toolbarLabel hidden"></span>
                </div>

                <div id="findbarMessageContainer">
                    <span id="findMsg" class="toolbarLabel"></span>
                </div>
            </div>  <!-- findbar -->



            <div class="toolbar">
                <div id="toolbarContainer">
                    <div id="toolbarViewer">
                        <div id="toolbarViewerLeft">
                            <button id="sidebarToggle" class="toolbarButton" title="Toggle Sidebar">
                                <span data-l10n-id="toggle_sidebar_label">Toggle Sidebar</span>
                            </button>
                            <div class="toolbarButtonSpacer"></div>
                            <button id="viewFind" class="toolbarButton" title="Find in Document">
                                <span data-l10n-id="findbar_label">Find</span>
                            </button>
                            <div class="splitToolbarButton hiddenSmallView">
                                <button class="toolbarButton pageUp" title="Previous Page" id="previous">
                                    <span data-l10n-id="previous_label">Previous</span>
                                </button>
                                <div class="splitToolbarButtonSeparator"></div>
                                <button class="toolbarButton pageDown" title="Next Page" id="next">
                                    <span data-l10n-id="next_label">Next</span>
                                </button>
                            </div>
                            <input type="number" id="pageNumber" class="toolbarField pageNumber" title="Page" value="1" size="4" min="1">
                            <span id="numPages" class="toolbarLabel"></span>
                        </div>
                        <div id="toolbarViewerMiddle">
                            <div class="splitToolbarButton">
                                <button id="zoomOut" class="toolbarButton zoomOut" title="Zoom Out">
                                    <span data-l10n-id="zoom_out_label">Zoom Out</span>
                                </button>
                                <div class="splitToolbarButtonSeparator"></div>
                                <button id="zoomIn" class="toolbarButton zoomIn" title="Zoom In">
                                    <span data-l10n-id="zoom_in_label">Zoom In</span>
                                </button>
                            </div>
                            <span id="scaleSelectContainer" class="dropdownToolbarButton">
                              <select id="scaleSelect" title="Zoom">
                                <option id="pageAutoOption" title="" value="auto" selected="selected" data-l10n-id="page_scale_auto">Automatic Zoom</option>
                                <option id="pageActualOption" title="" value="page-actual" data-l10n-id="page_scale_actual">Actual Size</option>
                                <option id="pageFitOption" title="" value="page-fit" data-l10n-id="page_scale_fit">Page Fit</option>
                                <option id="pageWidthOption" title="" value="page-width" data-l10n-id="page_scale_width">Page Width</option>
                                <option id="customScaleOption" title="" value="custom" disabled="disabled" hidden="true"></option>
                                <option title="" value="0.5" data-l10n-id="page_scale_percent" data-l10n-args='{ "scale": 50 }'>50%</option>
                                <option title="" value="0.75" data-l10n-id="page_scale_percent" data-l10n-args='{ "scale": 75 }'>75%</option>
                                <option title="" value="1" data-l10n-id="page_scale_percent" data-l10n-args='{ "scale": 100 }'>100%</option>
                                <option title="" value="1.25" data-l10n-id="page_scale_percent" data-l10n-args='{ "scale": 125 }'>125%</option>
                                <option title="" value="1.5" data-l10n-id="page_scale_percent" data-l10n-args='{ "scale": 150 }'>150%</option>
                                <option title="" value="2" data-l10n-id="page_scale_percent" data-l10n-args='{ "scale": 200 }'>200%</option>
                                <option title="" value="3" data-l10n-id="page_scale_percent" data-l10n-args='{ "scale": 300 }'>300%</option>
                                <option title="" value="4" data-l10n-id="page_scale_percent" data-l10n-args='{ "scale": 400 }'>400%</option>
                              </select>
                            </span>
                        </div>
                        <div id="toolbarViewerRight">
                            <button id="presentationMode" class="toolbarButton presentationMode hiddenLargeView" title="Switch to Presentation Mode">
                                <span data-l10n-id="presentation_mode_label">Presentation Mode</span>
                            </button>

                            <button id="openFile" class="toolbarButton openFile hiddenLargeView hidden" title="Open File">
                                <span data-l10n-id="open_file_label">Open</span>
                            </button>

                            <button id="print" class="toolbarButton print hiddenMediumView" title="Print">
                                <span data-l10n-id="print_label">Print</span>
                            </button>

                            <button id="download" class="toolbarButton download hiddenMediumView" title="Download">
                                <span data-l10n-id="download_label">Download</span>
                            </button>
                            <a href="#" id="viewBookmark" class="toolbarButton bookmark hiddenSmallView hidden" title="Current view (copy or open in new window)">
                                <span data-l10n-id="bookmark_label">Current View</span>
                            </a>

                            <div class="verticalToolbarSeparator hiddenSmallView"></div>

                            <button id="secondaryToolbarToggle" class="toolbarButton" title="Tools">
                                <span data-l10n-id="tools_label">Tools</span>
                            </button>
                        </div>
                        <div id="secondaryToolbar" class="secondaryToolbar hidden doorHangerRight">
                            <div id="secondaryToolbarButtonContainer">
                                <button id="secondaryPresentationMode" class="secondaryToolbarButton presentationMode visibleLargeView" title="Switch to Presentation Mode">
                                    <span data-l10n-id="presentation_mode_label">Presentation Mode</span>
                                </button>

                                <button id="secondaryOpenFile" class="secondaryToolbarButton openFile visibleLargeView" title="Open File">
                                    <span data-l10n-id="open_file_label">Open</span>
                                </button>

                                <button id="secondaryPrint" class="secondaryToolbarButton print visibleMediumView" title="Print">
                                    <span data-l10n-id="print_label">Print</span>
                                </button>

                                <button id="secondaryDownload" class="secondaryToolbarButton download visibleMediumView" title="Download">
                                    <span data-l10n-id="download_label">Download</span>
                                </button>

                                <a href="#" id="secondaryViewBookmark" class="secondaryToolbarButton bookmark visibleSmallView" title="Current view (copy or open in new window)">
                                    <span data-l10n-id="bookmark_label">Current View</span>
                                </a>

                                <div class="horizontalToolbarSeparator visibleLargeView"></div>

                                <button id="firstPage" class="secondaryToolbarButton firstPage" title="Go to First Page">
                                    <span data-l10n-id="first_page_label">Go to First Page</span>
                                </button>
                                <button id="lastPage" class="secondaryToolbarButton lastPage" title="Go to Last Page">
                                    <span data-l10n-id="last_page_label">Go to Last Page</span>
                                </button>

                                <div class="horizontalToolbarSeparator"></div>

                                <button id="pageRotateCw" class="secondaryToolbarButton rotateCw" title="Rotate Clockwise">
                                    <span data-l10n-id="page_rotate_cw_label">Rotate Clockwise</span>
                                </button>
                                <button id="pageRotateCcw" class="secondaryToolbarButton rotateCcw" title="Rotate Counterclockwise">
                                    <span data-l10n-id="page_rotate_ccw_label">Rotate Counterclockwise</span>
                                </button>

                                <div class="horizontalToolbarSeparator"></div>

                                <button id="cursorSelectTool" class="secondaryToolbarButton selectTool toggled" title="Enable Text Selection Tool">
                                    <span data-l10n-id="cursor_text_select_tool_label">Text Selection Tool</span>
                                </button>
                                <button id="cursorHandTool" class="secondaryToolbarButton handTool" title="Enable Hand Tool">
                                    <span data-l10n-id="cursor_hand_tool_label">Hand Tool</span>
                                </button>

                                <div class="horizontalToolbarSeparator"></div>

                                <button id="scrollVertical" class="secondaryToolbarButton scrollModeButtons scrollVertical toggled" title="Use Vertical Scrolling">
                                    <span data-l10n-id="scroll_vertical_label">Vertical Scrolling</span>
                                </button>
                                <button id="scrollHorizontal" class="secondaryToolbarButton scrollModeButtons scrollHorizontal" title="Use Horizontal Scrolling">
                                    <span data-l10n-id="scroll_horizontal_label">Horizontal Scrolling</span>
                                </button>
                                <button id="scrollWrapped" class="secondaryToolbarButton scrollModeButtons scrollWrapped" title="Use Wrapped Scrolling">
                                    <span data-l10n-id="scroll_wrapped_label">Wrapped Scrolling</span>
                                </button>

                                <div class="horizontalToolbarSeparator scrollModeButtons"></div>

                                <button id="spreadNone" class="secondaryToolbarButton spreadModeButtons spreadNone toggled" title="Do not join page spreads">
                                    <span data-l10n-id="spread_none_label">No Spreads</span>
                                </button>
                                <button id="spreadOdd" class="secondaryToolbarButton spreadModeButtons spreadOdd" title="Join page spreads starting with odd-numbered pages">
                                    <span data-l10n-id="spread_odd_label">Odd Spreads</span>
                                </button>
                                <button id="spreadEven" class="secondaryToolbarButton spreadModeButtons spreadEven" title="Join page spreads starting with even-numbered pages">
                                    <span data-l10n-id="spread_even_label">Even Spreads</span>
                                </button>

                                <div class="horizontalToolbarSeparator spreadModeButtons"></div>

                                <button id="documentProperties" class="secondaryToolbarButton documentProperties" title="Document Properties…">
                                    <span data-l10n-id="document_properties_label">Document Properties…</span>
                                </button>
                            </div>
                        </div>  <!-- secondaryToolbar -->
                    </div>
                    <div id="loadingBar">
                        <div class="progress">
                            <div class="glimmer">
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <menu type="context" id="viewerContextMenu">
                <menuitem id="contextFirstPage" label="First Page"
                          data-l10n-id="first_page"></menuitem>
                <menuitem id="contextLastPage" label="Last Page"
                          data-l10n-id="last_page"></menuitem>
                <menuitem id="contextPageRotateCw" label="Rotate Clockwise"
                          data-l10n-id="page_rotate_cw"></menuitem>
                <menuitem id="contextPageRotateCcw" label="Rotate Counter-Clockwise"
                          data-l10n-id="page_rotate_ccw"></menuitem>
            </menu>

            <div id="viewerContainer">
                <div id="viewer" class="pdfViewer"></div>
            </div>

            <div id="errorWrapper" hidden='true'>
                <div id="errorMessageLeft">
                    <span id="errorMessage"></span>
                    <button id="errorShowMore" data-l10n-id="error_more_info">
                        More Information
                    </button>
                    <button id="errorShowLess" data-l10n-id="error_less_info" hidden='true'>
                        Less Information
                    </button>
                </div>
                <div id="errorMessageRight">
                    <button id="errorClose" data-l10n-id="error_close">
                        Close
                    </button>
                </div>
                <div class="clearBoth"></div>
                <textarea id="errorMoreInfo" hidden='true' readonly="readonly"></textarea>
            </div>
        </div> <!-- mainContainer -->

        <div id="overlayContainer" class="hidden">
            <div id="passwordOverlay" class="container hidden">
                <div class="dialog">
                    <div class="row">
                        <p id="passwordText" data-l10n-id="password_label">Enter the password to open this PDF file:</p>
                    </div>
                    <div class="row">
                        <input type="password" id="password" class="toolbarField">
                    </div>
                    <div class="buttonRow">
                        <button id="passwordCancel" class="overlayButton"><span data-l10n-id="password_cancel">Cancel</span></button>
                        <button id="passwordSubmit" class="overlayButton"><span data-l10n-id="password_ok">OK</span></button>
                    </div>
                </div>
            </div>
            <div id="documentPropertiesOverlay" class="container hidden">
                <div class="dialog">
                    <div class="row">
                        <span data-l10n-id="document_properties_file_name">File name:</span> <p id="fileNameField">-</p>
                    </div>
                    <div class="row">
                        <span data-l10n-id="document_properties_file_size">File size:</span> <p id="fileSizeField">-</p>
                    </div>
                    <div class="separator"></div>
                    <div class="row">
                        <span data-l10n-id="document_properties_title">Title:</span> <p id="titleField">-</p>
                    </div>
                    <div class="row">
                        <span data-l10n-id="document_properties_author">Author:</span> <p id="authorField">-</p>
                    </div>
                    <div class="row">
                        <span data-l10n-id="document_properties_subject">Subject:</span> <p id="subjectField">-</p>
                    </div>
                    <div class="row">
                        <span data-l10n-id="document_properties_keywords">Keywords:</span> <p id="keywordsField">-</p>
                    </div>
                    <div class="row">
                        <span data-l10n-id="document_properties_creation_date">Creation Date:</span> <p id="creationDateField">-</p>
                    </div>
                    <div class="row">
                        <span data-l10n-id="document_properties_modification_date">Modification Date:</span> <p id="modificationDateField">-</p>
                    </div>
                    <div class="row">
                        <span data-l10n-id="document_properties_creator">Creator:</span> <p id="creatorField">-</p>
                    </div>
                    <div class="separator"></div>
                    <div class="row">
                        <span data-l10n-id="document_properties_producer">PDF Producer:</span> <p id="producerField">-</p>
                    </div>
                    <div class="row">
                        <span data-l10n-id="document_properties_version">PDF Version:</span> <p id="versionField">-</p>
                    </div>
                    <div class="row">
                        <span data-l10n-id="document_properties_page_count">Page Count:</span> <p id="pageCountField">-</p>
                    </div>
                    <div class="row">
                        <span data-l10n-id="document_properties_page_size">Page Size:</span> <p id="pageSizeField">-</p>
                    </div>
                    <div class="separator"></div>
                    <div class="row">
                        <span data-l10n-id="document_properties_linearized">Fast Web View:</span> <p id="linearizedField">-</p>
                    </div>
                    <div class="buttonRow">
                        <button id="documentPropertiesClose" class="overlayButton"><span data-l10n-id="document_properties_close">Close</span></button>
                    </div>
                </div>
            </div>
            <div id="printServiceOverlay" class="container hidden">
                <div class="dialog">
                    <div class="row">
                        <span data-l10n-id="print_progress_message">Preparing document for printing…</span>
                    </div>
                    <div class="row">
                        <progress value="0" max="100"></progress>
                        <span data-l10n-id="print_progress_percent" data-l10n-args='{ "progress": 0 }' class="relative-progress">0%</span>
                    </div>
                    <div class="buttonRow">
                        <button id="printCancel" class="overlayButton"><span data-l10n-id="print_progress_close">Cancel</span></button>
                    </div>
                </div>
            </div>
        </div>  <!-- overlayContainer -->

    </div><!-- outerContainer -->
    <div id="printContainer"></div>
</div>
<script src="/static/plugins/pdfjs/web/viewer.min.js"></script>