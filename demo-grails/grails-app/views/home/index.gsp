<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>DavKit Grails demo</title>
</head>
<body>
<h1 class="h3 mb-3">Documents</h1>
<p class="text-body-secondary">
    Each link is a signed URL (8&nbsp;h) rendered by the plugin's <code>&lt;davkit:editLink&gt;</code>
    taglib. Clicking hands the URL to Office, which opens, locks, edits and saves the row over WebDAV.
</p>
<table class="table table-striped align-middle">
    <thead>
    <tr>
        <th>Name</th>
        <th>Size</th>
        <th>Last updated</th>
        <th></th>
    </tr>
    </thead>
    <tbody>
    <g:each in="${documents}" var="doc">
        <tr>
            <td>${doc.name}</td>
            <td>${doc.bytes.length} bytes</td>
            <td><g:formatDate date="${doc.lastUpdated}" format="yyyy-MM-dd HH:mm:ss"/></td>
            <td><davkit:editLink path="documents/${doc.name}"/></td>
        </tr>
    </g:each>
    </tbody>
</table>
</body>
</html>
