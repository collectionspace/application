<div id="full-size-body">
  Welcome to Collection Space Toronto Demo 1.

<ul>
<#list types as type>
<li>
    ${type.name}: <a href='${type.createurl}'>create</a>
                  <a href='${type.searchurl}'>search</a>
                  <a href='${type.allurl}'>all</a>
</li>
</#list>
</ul>

</div>