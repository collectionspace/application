CollectionSpace is an open-source, web-based software application for the description, management, and dissemination of museum collections information. The CollectionSpace project team is made up of museum professionals, software engineers, and interaction designers.

CollectionSpace is licensed for use pursuant to the Educational Community License v2.0. Learn more about the ECL at http://opensource.org/licenses/ECL-2.0. The source code is freely available.

For more information about CollectionSpace see http://www.collectionspace.org/about/faq.

```
Audit Service
-----------------------------------------------------------------------------------------------------------------------------------------------
To require the Audit service for a specific tenant, set the 'audit-required' element of the tenant's 'settings.xml' file to 'true'.

Tenant's 'settings.xml' bindings file:
<settings>
	<persistence>
		<service>
			<tenant>
				<id>1500</id>
				...
				<audit-required>true</audit-required>
			</tenant>
		</service>
	</persistence>
</settings>
-----------------------------------------------------------------------------------------------------------------------------------------------
```
