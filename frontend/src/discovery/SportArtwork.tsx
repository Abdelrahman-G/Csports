import { useId } from 'react'

export default function SportArtwork({ name }: { name: string }) {
  const id = useId().replaceAll(':', '')
  const sport = name.toLowerCase()
  const racket = sport === 'tennis' || sport === 'padel'
  const ball = ['football', 'basketball', 'volleyball'].includes(sport)
  return <div className="sport-artwork" data-sport={sport} aria-hidden="true">
    <span className="sport-artwork-kicker">CSPORTS / DISCOVER</span>
    <svg className="sport-artwork-scene" key={name} viewBox="0 0 480 360" fill="none">
      <defs><radialGradient id={`${id}-light`} cx="30%" cy="25%"><stop stopColor="#fff8d9" /><stop offset=".6" stopColor="#e8b95a" /><stop offset="1" stopColor="#ac6a24" /></radialGradient></defs>
      <g className="sport-court-lines" stroke="currentColor" strokeWidth="1.5" opacity=".3"><path d="M-40 280 390-35M40 380 520 20M-40 140 330 420M80-50 520 280" /><ellipse cx="240" cy="180" rx="162" ry="112" transform="rotate(-32 240 180)" /></g>
      <ellipse className="sport-object-shadow" cx="260" cy="277" rx="100" ry="17" fill="#102e21" opacity=".16" />
      <g className="sport-object">
        {ball && <><circle cx="240" cy="168" r="85" fill={`url(#${id}-light)`} stroke="#533e22" strokeWidth="2" />
          {sport === 'football' ? <g fill="#243c30"><path d="m238 134 34 21-11 39h-40l-13-38Z" /><path d="m189 100 15 26-26 32-23-8a85 85 0 0 1 34-50Zm100 2-16 24 24 33 27-10a85 85 0 0 0-35-47Zm-86 141 5-31 39-4 19 30a85 85 0 0 1-63 5Z" /><path d="m180 156 29 1m62-2 27 2m-77 38-12 20m52-20 9 19m-32-79 1-34" stroke="#243c30" strokeWidth="2" /></g>
          : sport === 'basketball' ? <g stroke="#533e22" strokeWidth="3"><path d="M155 168h170M240 83v170M182 106c88 10 119 61 114 120M184 231c-5-80 45-137 110-127" /></g>
          : <g stroke="#533e22" strokeWidth="2"><path d="M240 83c-50 54-20 108 58 148M159 141c64-4 105 32 105 108M196 241c47-36 70-93 91-140M156 179c54-18 114-4 157 31" /></g>}</>}
        {sport === 'tennis' && <g transform="rotate(28 240 180)"><path d="M240 220v77" stroke="#183e2b" strokeWidth="18" strokeLinecap="round" /><ellipse cx="240" cy="143" rx="62" ry="85" fill="#f1ebc4" stroke="#183e2b" strokeWidth="9" /><g stroke="#527558" strokeWidth="1.5"><path d="M211 72v144M230 60v164M250 61v163M269 72v143M183 110h114M178 130h124M179 150h122M182 170h115M190 190h98" /></g><circle cx="329" cy="216" r="23" fill="#e8c84f" /><path d="M313 199q26 15 3 34" stroke="#ffffe4" strokeWidth="3" /></g>}
        {sport === 'padel' && <g transform="rotate(-22 240 180)">
          <path d="M240 218v68" stroke="#183e2b" strokeWidth="20" strokeLinecap="round" />
          <path d="M235 274q-38 30-9 43q24 10 29-17" stroke="#183e2b" strokeWidth="3" />
          <path d="M240 65c-48 0-77 32-71 78 4 35 28 66 56 88h30c28-22 52-53 56-88 6-46-23-78-71-78Z" fill="#e8b95a" stroke="#183e2b" strokeWidth="8" />
          <g fill="#183e2b">{[106, 132, 158, 184].map((y, row) =>
            [-1, 0, 1].map(column => <circle key={`${row}-${column}`} cx={240 + column * 27} cy={y} r="6" />),
          )}</g>
          <path d="m226 210 14 15 14-15" stroke="#183e2b" strokeWidth="5" />
          <circle cx="338" cy="230" r="22" fill="#e8d768" /><path d="M323 214q26 15 4 34" stroke="#ffffe4" strokeWidth="3" />
        </g>}
        {sport === 'swimming' && <g><path d="M84 197q39-29 78 0t78 0t78 0t78 0M84 224q39-29 78 0t78 0t78 0t78 0M84 251q39-29 78 0t78 0t78 0t78 0" stroke="#d9f3e8" strokeWidth="6" strokeLinecap="round" /><circle cx="276" cy="134" r="21" fill="#e8b95a" /><path d="m164 163 51-42 43 48-46 12M214 122l-39-26-37 23" stroke="#e8b95a" strokeWidth="18" strokeLinecap="round" strokeLinejoin="round" /></g>}
        {sport === 'running' && <g stroke="#e8b95a" strokeWidth="18" strokeLinecap="round" strokeLinejoin="round"><circle cx="265" cy="90" r="20" fill="#e8b95a" stroke="none" /><path d="m250 127-29 51 35 28-14 63m-23-88-35 40-43 1m88-78-33-7-25 25m75-34 20 27 36-4" /><path d="M115 259h75M96 239h38" stroke="#d9f3e8" strokeWidth="4" /></g>}
        {sport === 'boxing' && <g transform="rotate(-15 240 180)"><path d="M180 215v-93q0-45 51-45h31q44 0 44 43v67q0 39-35 50l-5 44h-77l-6-48q-33-10-37-43l-4-34q0-18 18-18l22 17" fill="#e8b95a" stroke="#71442a" strokeWidth="4" /><path d="M187 237h83v44h-83Z" fill="#183e2b" /><path d="M202 111h76M200 122h76" stroke="#fff1cf" strokeWidth="3" /></g>}
        {!ball && !racket && !['swimming','running','boxing'].includes(sport) && <g stroke="#e8b95a" strokeWidth="3"><circle cx="240" cy="165" r="91" /><ellipse cx="240" cy="165" rx="42" ry="91" transform="rotate(45 240 165)" /><ellipse cx="240" cy="165" rx="42" ry="91" transform="rotate(-45 240 165)" /><path d="m224 180 30-30m-27 0h27v27" strokeWidth="9" strokeLinecap="round" strokeLinejoin="round" /></g>}
      </g>
    </svg>
    <div className="sport-artwork-caption" key={`${name}-caption`}><span>{name}</span><small>Find a course. Make it part of your week.</small></div>
  </div>
}
