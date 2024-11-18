interface Props {
  children: React.ReactNode;
  label: string;
}

export default function ConfigurationGroup({ children, label }: Props) {
  return (
    <section className="flex flex-col gap-2">
      <h3 className="text-lg font-semibold">{label}</h3>
      <div className="border border-border rounded-lg shadow-sm">
        {children}
      </div>
    </section>
  );
}
