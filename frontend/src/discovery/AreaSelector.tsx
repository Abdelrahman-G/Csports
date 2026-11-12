import { useId, useState } from 'react'
import type { Region } from './types'

type Props = { regions: Region[]; value: string; onChange: (id: string) => void; onNearby: () => void; locating: boolean; nearby: boolean }

export default function AreaSelector({ regions, value, onChange, onNearby, locating, nearby }: Props) {
  const [query, setQuery] = useState('')
  const [expanded, setExpanded] = useState(false)
  const id = useId()
  const selected = regions.find(r => String(r.id) === value)
  const matches = regions.filter(r => `${r.name} ${r.city}`.toLowerCase().includes(query.trim().toLowerCase()))
  return <section className="area-explorer" aria-label="Choose an area">
    <div className="area-explorer-heading"><div><span className="area-step">02 / WHERE</span><h2>{nearby ? 'Around you' : selected ? selected.name : 'Across Cairo & Giza'}</h2></div>
      <div className="area-explorer-actions"><button type="button" className={nearby ? 'is-selected' : ''} onClick={onNearby} disabled={locating}>{locating ? 'Locating…' : 'Near me ↗'}</button><button type="button" aria-expanded={expanded} aria-controls={id} onClick={() => setExpanded(v => !v)}>{expanded ? 'Close search −' : 'Find an area +'}</button></div>
    </div>
    <div className={`area-search-drawer${expanded ? ' is-open' : ''}`} inert={!expanded} id={id}><div><label htmlFor={`${id}-query`}>Search areas</label><input id={`${id}-query`} type="search" value={query} onChange={event => setQuery(event.target.value)} placeholder="Area or city name" /></div></div>
    <div className="area-stations" role="group" aria-label="Areas"><button type="button" aria-pressed={!value && !nearby} onClick={() => onChange('')}><span aria-hidden="true" />All areas</button>
      {(expanded ? matches : regions).map(region => <button type="button" key={region.id} aria-pressed={!nearby && String(region.id) === value} onClick={() => onChange(String(region.id))}><span aria-hidden="true" />{region.name}<small>{region.city}</small></button>)}
    </div>
    {expanded && matches.length === 0 && <p className="area-no-match" role="status">No matching areas. Try another name.</p>}
  </section>
}
