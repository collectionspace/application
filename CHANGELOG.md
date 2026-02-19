# CollectionSpace Application Changelog

## 8.3.0

### Authorities

#### Chronology

* Remove `ui-type="enum"` from identifierCitation

### Procedures

#### Acquisition

* Add free text field `acquisitionDescription`
* Add parties involved group `partiesInvovledGroupList/partiesInvolvedGroup`

#### Consultation

* Add repeatable field `consultationOutcomes/consultationOutcome`

#### Deaccession

* Add parties involved group `partiesInvovledGroupList/partiesInvolvedGroup`

### CollectionObject

* **Anthro** Add material/technique description free text field
* Add repeating autocomplete field `controlledContentPlaces/controlledContentPlace`

### Media

* Add `mini="list"` to alt text

### Term Lists

#### Annotation Type

**All Profiles**

* Update default terms to catalog note, legacy data note, and staff note

#### Inventory Status

**Core, Anthro, Bonsai, BotGarden, FCART, Herbarium, LHMC, Materials**

* Update default terms to deaccessions, in storage, missing, off site, on loan, and on display

#### Organization Types

**Core, Anthro, Bonsai, BotGarden, FCART, Herbarium, LHMC, Materials**

* Add federally-recognized tribe and non-federally-recognized tribe to default terms

### Misc

* Added SMTP environment variables
* Added displayName to tenant binding output for authority records

## 8.2.0

### Procedures

### CollectionObject

* Add repeating field `objectProductionAgents/objectProductionAgent`

### Object Exit

* Add field `note`

### Held-in-Trust

* Add field `note`

### Summary Documentation

* Add field `note`

### NAGPRA Inventory

* Add field `note`

### Profiles

#### Anthro

* Remove `anthroOwnershipAccess` from `CollectionObject`

#### FCART

* Add `deaccessionreason` term list to fcart profile instance vocabularies
