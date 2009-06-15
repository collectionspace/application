<div id='sidebar-accordion-block'>
  <div class='sidebar-accordion-nav'>
    <#if clearable><a href="#0" class='clear'>clear search</a><br/></#if>
    <#if prev??><a href='#${prev}' class='back'>&laquo; previous page</a></#if>
    <#if next??><a href='#${next}' class='fwd'>next page &raquo;</a></#if>
    <br style="clear: both;"/>
  </div>
  <div id='sidebar-accordion'>
  <#list items as item>
    <h3><a href='#'>${item.title?html}</a></h3>
    <div>
    <div>
      ${item.brief?html}
    </div>
    <span class='sidebar-accordion-link'><a href='${item.url?html}'>view record &raquo;</a></span>
    <span class='record-id' style='display: none;'>${item.id?html}</span>
    </div>
  </#list>
  </div>
  <div class='sidebar-accordion-nav'>
    <#if prev??><a href='#${prev}' class='back'>&laquo; previous page</a></#if>
    <#if next??><a href='#${next}' class='fwd'>next page &raquo;</a></#if>
    <br style="clear: both;"/>
    <#if clearable><a href="#0" class='clear'>clear search</a><br/></#if>
  </div>
</div>
